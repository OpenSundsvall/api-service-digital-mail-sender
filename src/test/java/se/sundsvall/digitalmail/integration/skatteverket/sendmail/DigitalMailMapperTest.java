package se.sundsvall.digitalmail.integration.skatteverket.sendmail;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.w3._2000._09.xmldsig_.Signature;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.zalando.problem.ThrowableProblem;
import se.gov.minameddelanden.common.sign.X509KeySelector;
import se.gov.minameddelanden.schema.message.Attachment;
import se.gov.minameddelanden.schema.message.MessageBody;
import se.gov.minameddelanden.schema.message.Seal;
import se.gov.minameddelanden.schema.message.v3.MessageHeader;
import se.gov.minameddelanden.schema.message.v3.SealedDelivery;
import se.gov.minameddelanden.schema.message.v3.SignedDelivery;
import se.gov.minameddelanden.schema.service.DeliveryResult;
import se.gov.minameddelanden.schema.service.DeliveryStatus;
import se.gov.minameddelanden.schema.service.v3.DeliverSecure;
import se.gov.minameddelanden.schema.service.v3.DeliverSecureResponse;
import se.sundsvall.digitalmail.TestObjectFactory;
import se.sundsvall.digitalmail.api.model.BodyInformation;
import se.sundsvall.digitalmail.api.model.DigitalMailRequest;
import se.sundsvall.digitalmail.api.model.DigitalMailResponse;
import se.sundsvall.digitalmail.domain.DigitalMailRequestDto;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.map;

@ExtendWith(value = {MockitoExtension.class, SoftAssertionsExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class DigitalMailMapperTest {
    
    @Autowired
    private DigitalMailMapper mapper;
    
    @Test
    void testCreateDeliverSecure(final SoftAssertions softly) {
        final SealedDelivery sealedDelivery = mapper.createDeliverSecure(TestObjectFactory.generateDigitalMailRequestDto()).getDeliverSecure();
        final Seal seal = sealedDelivery.getSeal();
        final SignedDelivery signedDelivery = sealedDelivery.getSignedDelivery();
        final Signature signature = sealedDelivery.getSignature();
        
        softly.assertThat(seal.getReceivedTime()).isNotNull();
        softly.assertThat(seal.isSignaturesOK()).isTrue();
    
        softly.assertThat(signedDelivery.getDelivery().getHeader().getCorrelationId()).isEqualTo("");
        softly.assertThat(signedDelivery.getDelivery().getHeader().getRecipient()).isEqualTo("recipientId");
        softly.assertThat(signedDelivery.getDelivery().getHeader().getSender().getName()).isEqualTo("Sundsvalls Kommun");
        softly.assertThat(signedDelivery.getDelivery().getHeader().getSender().getId()).isEqualTo("162120002411");
    
        final MessageHeader header = signedDelivery.getDelivery().getMessages().get(0).getHeader();
    
        softly.assertThat(header.getSubject()).isEqualTo("Some subject");
        softly.assertThat(header.getLanguage()).isEqualTo("svSE");
        softly.assertThat(header.getSupportinfo().getPhoneNumber()).isEqualTo("0701234567");
        softly.assertThat(header.getSupportinfo().getEmailAdress()).isEqualTo("email@somewhere.com");
        softly.assertThat(header.getSupportinfo().getURL()).isEqualTo("http://url.com");
        softly.assertThat(header.getSupportinfo().getText()).isEqualTo("support text");
    
        final MessageBody body = signedDelivery.getDelivery().getMessages().get(0).getBody();
        softly.assertThat(body.getBody()).isNotNull();
        softly.assertThat(body.getContentType()).isEqualTo("text/plain");
        
    }
    
    @Test
    void testMd5Sum() {
        String testString = "Some test string";
        String wanted = "C41E6CD1FEC10F345B366AA2839F6EF4";
    
        final String actual = mapper.createMd5Checksum(testString.getBytes(StandardCharsets.UTF_8));
    
        assertThat(actual).isEqualTo(wanted);
    }
    
    @Test
    void testCreateBodyBytes_forTextPlain() {
        final String BODY_CONTENT = "Some body";
        final byte[] bodyBytes = mapper.createBody(BodyInformation.builder()
                .withBody(BODY_CONTENT)
                .withContentType(MediaType.TEXT_PLAIN_VALUE)
                .build());
    
        assertThat(new String(bodyBytes)).isEqualTo(BODY_CONTENT);
    }
    
    @Test
    void testCreateBodyBytes_forTextHtml() {
        final String BODY_CONTENT = "<html>stuff</html>";
        //Sent in data is base64-encoded
        final byte[] encoded = Base64.getEncoder().encode(BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
    
        final byte[] bodyBytes = mapper.createBody(BodyInformation.builder()
                .withBody(new String(encoded))
                .withContentType(MediaType.TEXT_HTML_VALUE)
                .build());
        
        //Check that the text we sent in is the same as the bytes generated.
        assertThat(new String(bodyBytes)).isEqualTo(BODY_CONTENT);
    }
    
    @Test
    void testGetAliasFromKeystore() throws KeyStoreException {
        final KeyStore keyStore = TestObjectFactory.getKeyStore();
        final String alias = mapper.getAliasFromKeystore(keyStore, "kivra");
        
        assertThat(alias).isEqualTo("kivra");
    }
    
    @Test
    void testGetAliasFromKeystore_shouldThrowException_whenNotFound() throws KeyStoreException {
        final KeyStore keyStore = TestObjectFactory.getKeyStore();
    
        assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> mapper.getAliasFromKeystore(keyStore, "notFound"))
                .withMessage("Couldn't find certificate for notFound");
    }
    
    @Test
    void testCreateAttachments() {
        DigitalMailRequest.Attachment attachment = new DigitalMailRequest.Attachment();
        attachment.setBody("Ym9keQ==");
        attachment.setFilename("filename.pdf");
        attachment.setContentType(MediaType.APPLICATION_PDF_VALUE);
    
        DigitalMailRequest.Attachment attachment2 = new DigitalMailRequest.Attachment();
        attachment2.setBody("Ym9keTI=");
        attachment2.setFilename("filename2.pdf");
        attachment2.setContentType(MediaType.APPLICATION_PDF_VALUE);
    
        final List<Attachment> attachments = mapper.createAttachments(List.of(attachment, attachment2));
        
        assertThat(new String(attachments.get(0).getBody(), StandardCharsets.UTF_8)).isEqualTo("body");
        assertThat(attachments.get(0).getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(attachments.get(0).getFilename()).isEqualTo("filename.pdf");
    
        assertThat(new String(attachments.get(1).getBody(), StandardCharsets.UTF_8)).isEqualTo("body2");
        assertThat(attachments.get(1).getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(attachments.get(1).getFilename()).isEqualTo("filename2.pdf");
    }
    
    @Test
    void testCreateDigitalMailResponse() {
        DeliverSecureResponse deliveryResult = new DeliverSecureResponse()
                .withReturn(new DeliveryResult()
                        .withTransId("abc123")
                        .withStatuses(List.of(new DeliveryStatus()
                                .withDelivered(true))));
        final DigitalMailResponse response = mapper.createDigitalMailResponse(deliveryResult, "partyId");
        
        assertThat(response.getDeliveryStatus().getPartyId()).isEqualTo("partyId");
        assertThat(response.getDeliveryStatus().getTransactionId()).isEqualTo("abc123");
    }
    
    //TODO, implement validation of created message, so it works..
    @Disabled
    @Test
    void testCreateDeliverSecure_andValidateSignature() throws Exception {
        final DigitalMailRequestDto dto = TestObjectFactory.generateDigitalMailRequestDto();
        final DeliverSecure deliverSecure = mapper.createDeliverSecure(dto);
        final SealedDelivery sealedDelivery = deliverSecure.getDeliverSecure();
        
        var documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
        var documentBuilder = documentFactory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();
        
        var sealedDeliveryJAXBElement = new JAXBElement<>(new QName("http://minameddelanden.gov.se/schema/Message/v3", "sealedDelivery"), SealedDelivery.class, sealedDelivery);
        var jaxbContext = JAXBContext.newInstance(SignedDelivery.class, SealedDelivery.class, DeliverSecure.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        
        marshaller.marshal(sealedDeliveryJAXBElement, document);
        
        NodeList nodeList = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nodeList.getLength() == 0) {
            throw new Exception("Cannot find Signature element");
        }
        
        final XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
        
        DOMValidateContext validateContext = new DOMValidateContext(new X509KeySelector(), nodeList.item(0));
        
        final XMLSignature xmlSignature = signatureFactory.unmarshalXMLSignature(validateContext);
        
        Iterator<Reference> i = xmlSignature.getSignedInfo().getReferences().iterator();
        
        for (int j=0; i.hasNext(); j++) {
            boolean refValid = i.next().validate(validateContext);
            System.out.println("ref["+j+"] validity status: " + refValid);
        }
        
        final boolean validated = xmlSignature.validate(validateContext);
        
        System.out.println(validated);
        
        //assertThat(validated).isTrue();
    }
    
}