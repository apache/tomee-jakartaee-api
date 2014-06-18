/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package javax.xml.bind;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

@SuppressWarnings("UnusedDeclaration")
public final class DatatypeConverter {

    private static DatatypeConverterInterface converter = new DatatypeConverterImpl();

    private DatatypeConverter() {
        // no-op
    }

    public static void setDatatypeConverter(final DatatypeConverterInterface converter) {
        if (converter == null) {
            throw new IllegalArgumentException("The DatatypeConverterInterface parameter must not be null");
        }
        if (DatatypeConverter.converter == null) {

            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new JAXBPermission("setDatatypeConverter"));
            }

            DatatypeConverter.converter = new DatatypeConverterHelper(converter);
        }
    }

    public static String parseString(final String lexicalXSDString) {
        return converter.parseString(lexicalXSDString);
    }

    public static BigInteger parseInteger(final String lexicalXSDInteger) {
        return converter.parseInteger(lexicalXSDInteger);
    }

    public static int parseInt(final String lexicalXSDInt) {
        return converter.parseInt(lexicalXSDInt);
    }

    public static long parseLong(final String lexicalXSDLong) {
        return converter.parseLong(lexicalXSDLong);
    }

    public static short parseShort(final String lexicalXSDShort) {
        return converter.parseShort(lexicalXSDShort);
    }

    public static BigDecimal parseDecimal(final String lexicalXSDDecimal) {
        return converter.parseDecimal(lexicalXSDDecimal);
    }

    public static float parseFloat(final String lexicalXSDFloat) {
        return converter.parseFloat(lexicalXSDFloat);
    }

    public static double parseDouble(final String lexicalXSDDouble) {
        return converter.parseDouble(lexicalXSDDouble);
    }

    public static boolean parseBoolean(final String lexicalXSDBoolean) {
        return converter.parseBoolean(lexicalXSDBoolean);
    }

    public static byte parseByte(final String lexicalXSDByte) {
        return converter.parseByte(lexicalXSDByte);
    }

    public static QName parseQName(final String lexicalXSDQName, final NamespaceContext nsc) {
        return converter.parseQName(lexicalXSDQName, nsc);
    }

    public static Calendar parseDateTime(final String lexicalXSDDateTime) {
        return converter.parseDateTime(lexicalXSDDateTime);
    }

    public static byte[] parseBase64Binary(final String lexicalXSDBase64Binary) {
        return converter.parseBase64Binary(lexicalXSDBase64Binary);
    }

    public static byte[] parseHexBinary(final String lexicalXSDHexBinary) {
        return converter.parseHexBinary(lexicalXSDHexBinary);
    }

    public static long parseUnsignedInt(final String lexicalXSDUnsignedInt) {
        return converter.parseUnsignedInt(lexicalXSDUnsignedInt);
    }

    public static int parseUnsignedShort(final String lexicalXSDUnsignedShort) {
        return converter.parseUnsignedShort(lexicalXSDUnsignedShort);
    }

    public static Calendar parseTime(final String lexicalXSDTime) {
        return converter.parseTime(lexicalXSDTime);
    }

    public static Calendar parseDate(final String lexicalXSDDate) {
        return converter.parseDate(lexicalXSDDate);
    }

    public static String parseAnySimpleType(final String lexicalXSDAnySimpleType) {
        return converter.parseAnySimpleType(lexicalXSDAnySimpleType);
    }

    public static String printString(final String val) {
        return converter.printString(val);
    }

    public static String printInteger(final BigInteger val) {
        return converter.printInteger(val);
    }

    public static String printInt(final int val) {
        return converter.printInt(val);
    }

    public static String printLong(final long val) {
        return converter.printLong(val);
    }

    public static String printShort(final short val) {
        return converter.printShort(val);
    }

    public static String printDecimal(final BigDecimal val) {
        return converter.printDecimal(val);
    }

    public static String printFloat(final float val) {
        return converter.printFloat(val);
    }

    public static String printDouble(final double val) {
        return converter.printDouble(val);
    }

    public static String printBoolean(final boolean val) {
        return converter.printBoolean(val);
    }

    public static String printByte(final byte val) {
        return converter.printByte(val);
    }

    public static String printQName(final QName val, final NamespaceContext nsc) {
        return converter.printQName(val, nsc);
    }

    public static String printDateTime(final Calendar val) {
        return converter.printDateTime(val);
    }

    public static String printBase64Binary(final byte[] val) {
        return converter.printBase64Binary(val);
    }

    public static String printHexBinary(final byte[] val) {
        return converter.printHexBinary(val);
    }

    public static String printUnsignedInt(final long val) {
        return converter.printUnsignedInt(val);
    }

    public static String printUnsignedShort(final int val) {
        return converter.printUnsignedShort(val);
    }

    public static String printTime(final Calendar val) {
        return converter.printTime(val);
    }

    public static String printDate(final Calendar val) {
        return converter.printDate(val);
    }

    public static String printAnySimpleType(final String val) {
        return converter.printAnySimpleType(val);
    }

}
