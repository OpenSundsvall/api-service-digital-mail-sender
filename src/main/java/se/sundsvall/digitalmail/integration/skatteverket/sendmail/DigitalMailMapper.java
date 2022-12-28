package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.common.X509CertificateWithPrivateKey;
import se.gov.minameddelanden.common.Xml;
import se.gov.minameddelanden.schema.message.Attachment;
import se.gov.minameddelanden.schema.message.MessageBody;
import se.gov.minameddelanden.schema.message.Seal;
import se.gov.minameddelanden.schema.message.v2.SecureDeliveryHeader;
import se.gov.minameddelanden.schema.message.v3.MessageHeader;
import se.gov.minameddelanden.schema.message.v3.ObjectFactory;
import se.gov.minameddelanden.schema.message.v3.SealedDelivery;
import se.gov.minameddelanden.schema.message.v3.SecureDelivery;
import se.gov.minameddelanden.schema.message.v3.SecureMessage;
import se.gov.minameddelanden.schema.message.v3.SignedDelivery;
import se.gov.minameddelanden.schema.message.v3.SupportInfo;
import se.gov.minameddelanden.schema.sender.Sender;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.DeliveryStatus;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.domain.DigitalMailRequestDto;
import se.sundsvall.digitalmail.integration.skatteverket.SkatteverketProperties;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class DigitalMailMapper {
    
    private static final Logger LOG = LoggerFactory.getLogger(DigitalMailMapper.class);
    public static final String SENDER_ID = "162120002411";
    public static final String SENDER_NAME = "Sundsvalls Kommun";
    
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final SkatteverketProperties properties;
    
    private final DocumentBuilder documentBuilder;
    private final Marshaller marshaller;
    
    private final KeyStore keyStore;
    private final X509CertificateWithPrivateKey certificate;

    public DigitalMailMapper(final SkatteverketProperties properties)
            throws KeyStoreException, UnrecoverableEntryException, CertificateException, IOException, NoSuchAlgorithmException, JAXBException, ParserConfigurationException {
        
        this.properties = properties;
        
        // Load the KeyStore and get the signing key and certificate.
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(new PathMatchingResourcePatternResolver().getResource(properties.getKeyStoreLocation()).getInputStream(), properties.getKeyStorePassword().toCharArray());
        
        //Read certificate from keystore
        certificate = setupCertificate();
        
        // Create JAXB marshaller
        var jaxbContext = JAXBContext.newInstance(SignedDelivery.class, SealedDelivery.class, DeliverSecure.class);
        marshaller = jaxbContext.createMarshaller();

        // Create document builder
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
    }

    /**
     * Reads certificate information from a keystore
     * @return
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnrecoverableEntryException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    private X509CertificateWithPrivateKey setupCertificate()
            throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException {
        
        var privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(getAliasFromKeystore(keyStore, "kivra"), new KeyStore.PasswordProtection(properties.getKeyStorePassword().toCharArray()));
        var cert = (X509Certificate) privateKeyEntry.getCertificate();
        
        return new X509CertificateWithPrivateKey(cert, privateKeyEntry.getPrivateKey());
    }
    
    public DigitalMailResponse createDigitalMailResponse(DeliverSecureResponse deliveryResult, String partyId) {
        final DigitalMailResponse digitalMailResponse = new DigitalMailResponse();
        digitalMailResponse.setDeliveryStatus(DeliveryStatus.builder()
                .withTransactionId(deliveryResult.getReturn().getTransId())
                .withDelivered(deliveryResult.getReturn().getStatuses().get(0).isDelivered())   //Will always be only one, for now
                .withPartyId(partyId)
                .build());
    
        return digitalMailResponse;
    }
    
    /**
     *
     * @param dto to map to a request
     * @return
     */
    public DeliverSecure createDeliverSecure(DigitalMailRequestDto dto) {
        final SealedDelivery sealedDelivery = createSealedDelivery(dto);
        
        return new DeliverSecure().withDeliverSecure(sealedDelivery);
    }

    /**
     * The sealed delivery to be inserted into the SealedDelivery-object
     *
     * @param dto
     * @return A Sealed delivery signed by not the sender but us as a mediator.
     */
    SealedDelivery createSealedDelivery(DigitalMailRequestDto dto) {
        try {
            //Get the correct certificate
            //Create the signedDeliveryDocument, inner one.
            var signedDelivery = createSignedDelivery(dto);

            var signedDeliveryElement = new JAXBElement<>(new QName("http://minameddelanden.gov.se/schema/Message/v3", "SignedDelivery"), SignedDelivery.class, signedDelivery);
            var signedDeliveryDocument = documentBuilder.newDocument();

            marshaller.marshal(signedDeliveryElement, signedDeliveryDocument);

            var xml = Xml.fromDOM(signedDeliveryDocument);
            var signedXml = xml.sign(certificate);
            signedDelivery = signedXml.toJaxbObject(SignedDelivery.class);

            var sealedDelivery = objectFactory.createSealedDelivery()
                .withSeal(new Seal()
                    .withSignaturesOK(true)
                    .withReceivedTime(createTimestamp()))
                .withSignedDelivery(signedDelivery);

            var sealedDeliveryElement = new JAXBElement<>(new QName("http://minameddelanden.gov.se/schema/Message/v3", "SealedDelivery"), SealedDelivery.class, sealedDelivery);
            var sealedDeliveryDocument = documentBuilder.newDocument();

            marshaller.marshal(sealedDeliveryElement, sealedDeliveryDocument);

            xml = Xml.fromDOM(sealedDeliveryDocument);
            signedXml = xml.sign(certificate);
            sealedDelivery = signedXml.toJaxbObject(SealedDelivery.class);

            return sealedDelivery;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The object to be digitally signed
     *
     * @param dto to be translated into a {@link SignedDelivery}
     * @return A Signed delivery, should be signed by the sender, which in this case is also us.
     */
    SignedDelivery createSignedDelivery(DigitalMailRequestDto dto) {
        return objectFactory.createSignedDelivery()
                .withDelivery(createSecureDelivery(dto));
    }

    XMLGregorianCalendar createTimestamp() {
        try {
            GregorianCalendar now = new GregorianCalendar();
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(now);
        } catch (DatatypeConfigurationException e) {
            LOG.error("Couldn't create XMLGregorianCalendar instance");
            //Should never happen but..
            throw Problem.builder()
                    .withTitle("Couldn't create XMLGregorianCalendar for SealedDelivery")
                    .withStatus(Status.INTERNAL_SERVER_ERROR)
                    .withCause(((ThrowableProblem) e.getCause()))
                    .build();
        }
    }
    
    SecureDelivery createSecureDelivery(DigitalMailRequestDto dto) {
        return new SecureDelivery()
                .withHeader(createSecureDeliveryHeader(dto))
                .withMessages(createSecureMessage(dto));
    }
    
    SecureMessage createSecureMessage(DigitalMailRequestDto dto) {
        return new SecureMessage()
                .withHeader(createMessageHeader(dto))
                .withBody(createMessageBody(dto))
                .withAttachments(createAttachments(dto.getAttachments()));
    }
    
    List<Attachment> createAttachments(final List<DigitalMailRequest.Attachment> attachments) {
        List<Attachment> listOfAttachments = null;
        
        //Check that we have any attachments
        if(attachments != null && !attachments.isEmpty()) {
            
            listOfAttachments = new ArrayList<>();
            
            for (DigitalMailRequest.Attachment attachment : attachments) {
                //We need to decode the base64-encoded string before we convert it to a byte array.
                final byte[] attachmentBytes = Base64.getDecoder()
                        .decode(attachment.getBody()
                                .getBytes(StandardCharsets.UTF_8));
                
                Attachment mailAttachment = new Attachment()
                        .withBody(attachmentBytes)
                        .withContentType(attachment.getContentType())
                        .withFilename(attachment.getFilename())
                        .withChecksum(createMd5Checksum(attachmentBytes));
        
                listOfAttachments.add(mailAttachment);
            }
        }
        
        return listOfAttachments;
    }
    
    String createMd5Checksum(byte[] attachmentBodyBytes) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(attachmentBodyBytes);
            final byte[] digest = md5.digest();
            return DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Couldn't create MD5-checksum for attachment", e);
            throw Problem.builder()
                    .withTitle("Couldn't create MD5-checksum for attachment")
                    .withStatus(Status.INTERNAL_SERVER_ERROR)
                    .withCause(((ThrowableProblem) e.getCause()))
                    .build();
        }
    }
    
    /**
     * Creates the &lt;v3:header&gt;-element
     * @param dto
     * @return
     */
    MessageHeader createMessageHeader(DigitalMailRequestDto dto) {
        return new MessageHeader()
                .withSubject(dto.getHeaderSubject())
                .withSupportinfo(createSupportInfo(dto))
                .withLanguage("svSE")
                .withId(RequestId.get());
    }
    
     /**
     * Creates the Supportinfo-element
     * @param dto
     * @return
     */

    SupportInfo createSupportInfo(DigitalMailRequestDto dto) {
        return new SupportInfo()
                .withText(dto.getSupportInfo().getSupportText())
                .withURL(dto.getSupportInfo().getContactInformationUrl())
                .withPhoneNumber(dto.getSupportInfo().getContactInformationPhoneNumber())
                .withEmailAdress(dto.getSupportInfo().getContactInformationEmail());
    }
    
    /**
     * Creates thebody-element
     * @param dto
     * @return
     */

    MessageBody createMessageBody(DigitalMailRequestDto dto) {
        MessageBody messageBody = new MessageBody();
        if(dto.getBodyInformation() != null) {
            messageBody
                    .withBody(createBody(dto.getBodyInformation()))
                    .withContentType(dto.getBodyInformation().getContentType());
        } else {
            //Create an "empty" body.
            messageBody
                    .withBody(new byte[0])
                    .withContentType(MediaType.TEXT_PLAIN_VALUE);
        }
        
        return messageBody;
    }
    
    byte[] createBody(BodyInformation bodyInformation) {
        
        if(bodyInformation.getContentType().equals(MediaType.TEXT_PLAIN_VALUE)){
            //If it's regular text, encode it.
            return bodyInformation.getBody().getBytes(StandardCharsets.UTF_8);
        } else {
            //If it's text/html we need to first decode the content and then "encode" it..
            return Base64.getDecoder().decode(bodyInformation.getBody());
        }
    }
    
    SecureDeliveryHeader createSecureDeliveryHeader(DigitalMailRequestDto dto) {
        return new SecureDeliveryHeader()
                .withSender(createSender())
                .withRecipient(dto.getRecipientId())
                .withAttention("")      //TODO, behöver vi attention, t.ex. avdelning hos en myndighet?
                .withCorrelationId(""); //TODO behövs? referens till t.ex. ett ärende eller likn.
    }
    
    Sender createSender() {
        return new Sender()
                .withId(SENDER_ID)
                .withName(SENDER_NAME);
    }

    /**
     * Retrieve the alias for the key from the keystore.
     * As we only have one key we get the first one, if we need to get more we need to find it by alias.
     * @param keyStore
     * @return
     * @throws KeyStoreException
     */
    String getAliasFromKeystore(KeyStore keyStore, String wantedAlias) throws KeyStoreException {
        final Enumeration<String> aliases = keyStore.aliases();
        
        String alias = "";
        boolean foundAlias = false;

        //Find the aliases and stop when we get the one we want.
        while(aliases.hasMoreElements()) {
            alias = aliases.nextElement();

            if(alias.equalsIgnoreCase(wantedAlias)) {
                foundAlias = true;
                LOG.info("Found keystore-entry with alias: {}", alias);
                break;
            }
        }

        if(!foundAlias) {
            throw Problem.builder()
                    .withTitle("Couldn't find certificate for " + wantedAlias)
                    .withStatus(Status.INTERNAL_SERVER_ERROR)
                    .build();
        }

        return alias;
    }
}
