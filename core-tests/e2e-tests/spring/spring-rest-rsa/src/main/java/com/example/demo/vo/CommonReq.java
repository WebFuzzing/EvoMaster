package com.example.demo.vo;

import com.example.demo.util.CryptoUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonReq<T> implements Serializable {

    private String appId;
    /**
     * Encrypt data using AES symmetric encryption
     */
    private String data;

    private String requestId;

    private String timestamp;
    /**
     * The AES encryption key, which is encrypted by your RSA public key.
     */
    private String key;
    /**
     * Signature of the data, used to verify the integrity of the request data
     */
    private String sign;

    /**
     * Plaintext data, used to store the request data after decryption
     */
    private T bizData;

    /**
     * Get the signature text of the request data
     * @return
     * @throws Exception
     */
    public String signText()  {

        SortedMap<String, String> map = new TreeMap<>();
        map.put("appId", this.appId);
        map.put("data", this.data);
        map.put("requestId", this.requestId);
        map.put("timestamp", this.timestamp);
        map.put("key", this.key);

        return CryptoUtil.getSortedData(map);
    }
}
