package se.gov.minameddelanden.common;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

public final class JAXBMarshal {

    private JAXBMarshal() { }

	public static <T> T deserialize(final byte[] bytes, final Class<T> objectClass) throws JAXBException {
		var jaxbContext = JAXBContext.newInstance(objectClass.getPackage().getName());
		var unmarshaller = jaxbContext.createUnmarshaller();
		var is = new ByteArrayInputStream(bytes);
		var elem = unmarshaller.unmarshal(new StreamSource(is), objectClass);

		return objectClass.cast(elem.getValue());
	}
}
