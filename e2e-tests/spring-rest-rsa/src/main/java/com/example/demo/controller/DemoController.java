package com.example.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.util.CryptoUtil;
import com.example.demo.vo.BindCardReq;
import com.example.demo.vo.BindCardResp;
import com.example.demo.vo.CommonReq;
import com.example.demo.vo.CommonResp;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DemoController {
    // Your private key
    public static final String YOUR_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCLhvv7oRCi/Y/d1yHWtePo0dFFpqgR5iITMa1TKolgUWkmeXAE72UgcGFEV3Vsb1k1aOkhsslhwyWTaxBgiEGhSMLswbMNXiGLsoV56kH0Mdms2zuRViUm067PUf/8sbP7a0z9/lsHnw3RQFp59bT7IuB2c6B5qpxkds59F1dR4cDjEYbee1EVXvMyRIXz10mG5VWb1M8+2njoTpZ/syKybPiaXUkyG7JX93l9Ax8MMV5T1H7GHnA9ZEygq5sDHj3se0hK3KLQS9xmEni+OFLfmEe8luhbF3ojbAeH31Iz3iXzQmxZb3HupHv1i/K30+YDdP4FgzPLuQsjFwjuM5WpAgMBAAECggEAYdF+s4jV2w8dX4/Fn3vhjoYay1PtnK7U0NQRCa9WpHou19RnXm5fXYCsEHeoUR83UaR9XSy01p8zpsh2sNaV/HbAga/C0epeZkwAG9rJ5mNUkkUY2+mjHjdl5N8+MnB7GBa/4YoDU7KEw2D0jXBfM3neF+00YlfkUOKiHTzR8QrhIkM9KUKICHBA6duSuOb8fMD5RTbfTsBjo8aKri2aU8oPEycxlx+GGxTgIGdaE7B97SUR1P/EJAThiW45+FEjH2MxD4+JjHPBfWVQSihAqOWNNFwnCICtgQ/N+dJbxQczaKycRT5MLDV27N+6UO7cIPOi6EhUVqS4LYyV0w8aoQKBgQDiJRSpyxTXI9V9gDLBx51NVcgkygida42DLh1u4jOy5EcSCMWeLlumxnOPQeqUZOzmdrlv8LkrevUqAWyGTxYqUOk2rsqWar9Lyvs62WFn3WRjbUUiMsWxnOpexCkK1ETXfdC6ZJFsmIowKdB84qoLww+zFVHwLCZESJuoz4sRvQKBgQCd8ocAdv3akx3Il3HgOr+Fcq97jtuUtw2yKcsWt7eHx0JbPA2mxGd7FuAcFG5G82K5mfAsGJfdbUfj55SOE01tAltLwJh6RZULn7MhUysgADIY39IsS5n4kQp8FMHH+/K4OPqzDKT1SW8/eB5rUhY+xGF7gIy/vAF18zSEcD70XQKBgQDIBuRwCyEz6o47o9lBbb7FWMrfP5S/KRLSpUeDfLEd2qzCVt/1Oiv3KDGu1S8YcuzYLMt5KAOhYfDYZsoHQozogQjHRXQL9/+cmr39H6n4pOrWxyAPT7ltkM39ZKSo33jE4pRtSecXlxUj5Nh0nkiqfq60SHdhaKuwWkjU2D66QQKBgDMempkI6hJDCSGx+lZDTVdIjgkkbGcOc+1U33kjzs+wKwbSQezWplNNTQ1pg2ONREejzfrHnuc4hkr52be+AZIlcBztYaw5NwsDDfvcKhn6Vjx8vE4/zb6IWudb5HfwUFdVgbZPglgtA0d8fgPoFnEMKCzLp0Iq/CILq9Sta4K1AoGAFGE+cHm+CqKOg6nNkRgh6mnZv1wlVEtCrL+xSZSSyDvFFavGCctEUOnSPSer0hML9Oq4u/iV+ecYYAP3i39v3k1KTH5+BRQwzUvqTXBKZvEI0HW7OEtemEdb/5nPkMCtFPi/A1sRVaFFUI8CVvq0Np8++pupp9ZZ/x6/l8kGvWo=";
    // Your public key
    public static final String YOUR_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi4b7+6EQov2P3dch1rXj6NHRRaaoEeYiEzGtUyqJYFFpJnlwBO9lIHBhRFd1bG9ZNWjpIbLJYcMlk2sQYIhBoUjC7MGzDV4hi7KFeepB9DHZrNs7kVYlJtOuz1H//LGz+2tM/f5bB58N0UBaefW0+yLgdnOgeaqcZHbOfRdXUeHA4xGG3ntRFV7zMkSF89dJhuVVm9TPPtp46E6Wf7Mismz4ml1JMhuyV/d5fQMfDDFeU9R+xh5wPWRMoKubAx497HtIStyi0EvcZhJ4vjhS35hHvJboWxd6I2wHh99SM94l80JsWW9x7qR79Yvyt9PmA3T+BYMzy7kLIxcI7jOVqQIDAQAB";
    // The other party's private key
    public static final String OTHER_PARTY_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC8JkW53mBe8dfY07xV4Oq/Oho7PV8YrYjmla7wXdhJrCSPf6Dk+TZoKt0rLtHVmkqsUDEPX0L3o51NU/nrgHL/0Zy4Q72+bWPbArPKW2TdXPceARtYS+iflKgDJQuHHan3Qg6Q0xY2fljI0+zD8tTlBfQa42XQ0+tWVCXYA8qofq9TebgWxa6cyW65J6Ceg/EEWf3ILdzvE6mwY+N/3M4WgaSE4m9cD/LhXbm81H5BBmQ1TgLQKd1oYXp4CMvmtxpXTe9YfdPGgM8EF1UhzhzAqWiBmgt4Dsn66YKTZXv4RChr/NO5rFjnBzHx5Y3+RuKyxWmKzUJmLy2ZJ2q2wbf5AgMBAAECggEAf9UFBK7JDVkFTQU27aY9+CTS07Lz3IFzBS9nx1oLbKqvwGt3dnq383hEAmXyHU2SNJbbblVL25LnejV3FQGVoKfa8frcfPg3owKoAvGrixbxswoPeSNB7sKCkHhn56pI43yXBuDUM7wOOIx8Tfm7mcs8udQMRiDlwSa0+5o/48/cd4S0PQ0EeqHoBqJ8+05CsTv4rRGn7TIZ/y5P7gZ8F+IM6MKO3L8wu+9FRwKcomlBDYEYn2v7GYlndlzXQQ6wR02XBNF0LXS0TETywHXRCYB6/T70Gg1E1bf8/IQ4GmWhiD4FBru+IaTK3h6/RJPn1ZZl+HBsaYY67FItayJ2gQKBgQDkouP3s4p6n+DGvyo5W49MAIuNdzhlsex2zm+VwrpbiYrReYPIcUJtmepcWIAyv1obQqG9zoG/bG+NfZI4YPuydXRQW/W5I321STprmE0HvD6nR4CUqkc0uKKwn5c1YOtinHkAYxqkFrvTqSh+RYJh8JK8E+PHBcgAflIgZGPFsQKBgQDSquvAUe4aBS+EvF8WJryQ038t2pcFjvRMheIGpAlRBjR7No/LK7B45+WbvGXj0UF3NVdYefgiF4nzAUe3O2THYl2LCLHK2p1N8I3vB/gBAf5asfS06G/lIl/q6EKL49TdzxqDgSfcsch/04AmoAtlPVAE1/bMWDTg6WH3WIOAyQKBgFckqsUXfnl8hHzcEejot/Zc4tPZk/pW1Wz3A9rN7J/FDc8HhK4aVRpXJtdpt/sfqeVoASPKGPPwDZidOuUYOfbsA5g5ZK/bEifNsGGgHAQNwMebXivLtvYmRYQSX9ytgyoDv67hFx9httdWyyNNtQNFYXgkEJYj4vYlL4I/ITQRAoGBALgFA3/wUVs7UmHRJgI0fhzIElzch3UchXveqyx/13+GOwuyAnNHy5QhhOi/7gTNwjC+UHkBueUVyLOTV7DP2d1sqCeNxbhbtHLjgSfePx2qPyO8NPRd4Xg2ybBph8+oiUXj5dYfWNGoHmrCNjwHK00Y/K/uci/XkQEx/BhSojQ5AoGBAN59nShmL/9nopOs9gkhlF0ygSI3hltswtoERQJMTE/XHMo3+rA8BhOdQgzyZwvMf0yLvjmkwar1OtwRV0YVOaqD8Z7d49chz8PiszEkBeCAGnsMQzvszGqm/jvA+qMQPNQ5s0mt50VWmFA2lHF2FOps27Mma6W8AwZGxeznDrvZ";
    // The other party's public key
    public static final String OTHER_PARTY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvCZFud5gXvHX2NO8VeDqvzoaOz1fGK2I5pWu8F3YSawkj3+g5Pk2aCrdKy7R1ZpKrFAxD19C96OdTVP564By/9GcuEO9vm1j2wKzyltk3Vz3HgEbWEvon5SoAyULhx2p90IOkNMWNn5YyNPsw/LU5QX0GuNl0NPrVlQl2APKqH6vU3m4FsWunMluuSegnoPxBFn9yC3c7xOpsGPjf9zOFoGkhOJvXA/y4V25vNR+QQZkNU4C0CndaGF6eAjL5rcaV03vWH3TxoDPBBdVIc4cwKlogZoLeA7J+umCk2V7+EQoa/zTuaxY5wcx8eWN/kbissVpis1CZi8tmSdqtsG3+QIDAQAB";

    @PostMapping("/bind_card_apply")
    public CommonResp<BindCardResp> bindCardApply(@RequestBody CommonReq<BindCardReq> req) throws Exception {
        // verify the signature
        String signContent = req.signText();
        String publicKey = OTHER_PARTY_PUBLIC_KEY;
        boolean checkSign = CryptoUtil.verify(signContent, req.getSign(), CryptoUtil.getPublicKey(publicKey));
        if (!checkSign) {
            System.out.println("------ERROR! Invalid sign for the req:{}" + JSONObject.toJSONString(req));
            return handle(CommonResp.of("ERROR", "invalid signature", null));
        }
        // decrypt the biz data with your private key
        String bizData = null;
        try {
            String privateKey = YOUR_PRIVATE_KEY;
            String aesKeyPlainText = CryptoUtil.decryptByPrivateRSA(req.getKey(), privateKey);
            bizData = CryptoUtil.decrypt(req.getData(), aesKeyPlainText);
        } catch (Exception ex) {
            System.out.println("------ERROR! Decryption failed");
            return handle(CommonResp.of("ERROR", "Decryption failed", null));
        }
        System.out.println("------Decrypted bizData:{}" + bizData);

        // generate the response
        BindCardResp resp = new BindCardResp();
        resp.setSessionId(UUID.randomUUID().toString());
        return handle(CommonResp.of("OK", "request successful", resp));
    }

    private CommonResp handle(CommonResp commonResp) {
        CommonResp result = new CommonResp();

        String paramsPlainText = commonResp.getBizData() == null ? "{}" : JSONObject.toJSONString(commonResp.getBizData());

        String paramsCipherText = null;
        try {
            paramsCipherText = CryptoUtil.encrypt(paramsPlainText, commonResp.getKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        result.setData(paramsCipherText);

        // encrypt the aes key with other party's public key
        String publicKey = OTHER_PARTY_PUBLIC_KEY;
        try {
            result.setKey(CryptoUtil.encryptByPublicKey(commonResp.getKey(), publicKey));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String signText = commonResp.signText();

        // sign the response
        String privateKey = YOUR_PRIVATE_KEY;
        String sign = null;
        try {
            sign = CryptoUtil.sign(signText, privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        result.setSign(sign);
        return result;
    }


}