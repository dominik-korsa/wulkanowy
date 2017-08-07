package io.github.wulkanowy.security;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

public class Scrambler {

    private KeyStore keyStore;
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public final static String DEBUG_TAG = "KeyStoreSecurity";
    public Context context;

    public Scrambler(Context context) {
        this.context = context;
    }

    public void loadKeyStore() throws CryptoException {

        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, e.getMessage());
            throw new CryptoException(e.getMessage());
        }

    }

    public ArrayList<String> getAllAliases() throws CryptoException {

        ArrayList<String> keyAliases = new ArrayList<>();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                keyAliases.add(aliases.nextElement());
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, e.getMessage());
            throw new CryptoException(e.getMessage());
        }

        return keyAliases;
    }

    @TargetApi(18)
    public void generateNewKey(String alias) throws CryptoException {

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        AlgorithmParameterSpec spec;

        end.add(Calendar.YEAR, 10);
        if (!alias.isEmpty()) {
            try {
                if (!keyStore.containsAlias(alias)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        spec = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                                .setDigests(KeyProperties.DIGEST_SHA256)
                                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                                .setCertificateNotBefore(start.getTime())
                                .setCertificateNotAfter(end.getTime())
                                .build();

                    } else {
                        spec = new KeyPairGeneratorSpec.Builder(context)
                                .setAlias(alias)
                                .setSubject(new X500Principal("CN=" + alias))
                                .setSerialNumber(BigInteger.TEN)
                                .setStartDate(start.getTime())
                                .setEndDate(end.getTime())
                                .build();
                    }

                    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE);
                    keyPairGenerator.initialize(spec);
                    keyPairGenerator.generateKeyPair();

                } else {
                    Log.w(DEBUG_TAG, "GenerateNewKey - " + alias + " is exist");
                }
            } catch (Exception e) {
                Log.e(DEBUG_TAG, e.getMessage());
                throw new CryptoException(e.getMessage());
            }
        } else {
            Log.e(DEBUG_TAG, "GenerateNewKey - String is empty");
            throw new CryptoException("GenerateNewKey - String is empty");
        }


        Log.d(DEBUG_TAG, "Key pair are create");

    }

    public void deleteKey(String alias) throws CryptoException {

        if (!alias.isEmpty()) {
            try {
                keyStore.deleteEntry(alias);
                Log.d(DEBUG_TAG, "Key" + alias + "is delete");
            } catch (Exception e) {
                Log.e(DEBUG_TAG, e.getMessage());
            }
        } else {
            Log.e(DEBUG_TAG, "DeleteKey - String is empty");
            throw new CryptoException("DeleteKey - String is empty");
        }
    }

    public String encryptString(String alias, String text) throws CryptoException {

        if (!alias.isEmpty() && !text.isEmpty()) {
            try {
                KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
                RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

                Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                input.init(Cipher.ENCRYPT_MODE, publicKey);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                CipherOutputStream cipherOutputStream = new CipherOutputStream(
                        outputStream, input);
                cipherOutputStream.write(text.getBytes("UTF-8"));
                cipherOutputStream.close();

                Log.d(DEBUG_TAG, "String is encrypt");

                byte[] vals = outputStream.toByteArray();

                String encryptedText = Base64.encodeToString(vals, Base64.DEFAULT);
                Log.d(DEBUG_TAG, encryptedText);
                return encryptedText;

            } catch (Exception e) {
                Log.e(DEBUG_TAG, e.getMessage());
                throw new CryptoException(e.getMessage());
            }
        } else {
            Log.e(DEBUG_TAG, "EncryptString - String is empty");
            throw new CryptoException("EncryptString - String is empty");
        }
    }

    public String decryptString(String alias, String text) throws CryptoException {

        if (!alias.isEmpty() && !text.isEmpty()) {
            try {
                KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);

                Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());

                CipherInputStream cipherInputStream = new CipherInputStream(
                        new ByteArrayInputStream(Base64.decode(text, Base64.DEFAULT)), output);

                ArrayList<Byte> values = new ArrayList<>();

                int nextByte;

                while ((nextByte = cipherInputStream.read()) != -1) {
                    values.add((byte) nextByte);
                }

                Byte[] bytes = values.toArray(new Byte[values.size()]);

                Log.d(DEBUG_TAG, "String is decrypt");

                return new String(ArrayUtils.toPrimitive(bytes), 0, bytes.length, "UTF-8");

            } catch (Exception e) {
                Log.e(DEBUG_TAG, e.getMessage());
                throw new CryptoException(e.getMessage());
            }
        } else {
            Log.e(DEBUG_TAG, "EncryptString - String is empty");
            throw new CryptoException("EncryptString - String is empty");

        }
    }
}