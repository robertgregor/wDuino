import java.security.MessageDigest;
import java.util.Base64;

/*
 * The MIT License
 *
 * Copyright Error: on line 6, column 29 in Templates/Licenses/license-mit.txt
 The string doesn't match the expected date/time format. The string to parse was: "10.2.2015". The expected format was: "MMM d, yyyy". Remote-Home s.r.o..
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 *
 * @author gregorro
 */
public class OMAdmTest {
    public static void main(String... args) throws Exception {
        String nonce = "akFuUEtoYWhxQlZiMmtyNw==";
        String base64nonceDecode = new String(Base64.getDecoder().decode(nonce));
        System.out.println("Used nonce: " + base64nonceDecode);
        String basicDigest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest("SmartTrust:h3gPassword".getBytes()));
        //String basicDigest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(":".getBytes()));
        System.out.println("Encoded basic credentials: " + basicDigest);        
        String finalMD5checksum = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest((basicDigest+":"+base64nonceDecode).getBytes()));
        System.out.println("Final credentials: " + finalMD5checksum);
    }
}
