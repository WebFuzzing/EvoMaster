package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;
import com.example.demo.util.CryptoUtil;
import org.apache.commons.lang3.RandomStringUtils;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonResp<T extends BaseResult> implements Serializable {
    /**
     * Encrypt data using AES symmetric encryption
     */
    private String data;
    /**
     * The AES encryption key, which is encrypted by the RSA public key of the other party.
     */
    private String key;

    private String sign;
    /**
     * plaintext data
     */
    private T bizData;


    public static <T extends BaseResult> CommonResp<T> of(String code, String msg, T bizData) {
        if (bizData == null) {
            bizData = (T) new BaseResult();
        }
        bizData.setCode(code);
        bizData.setMsg(msg);
        String aesKey = RandomStringUtils.randomAlphanumeric(16);
        CommonResp commonResp = CommonResp.builder()
                .key(aesKey)
                .bizData(bizData)
                .build();
        return commonResp;
    }
    public String signText() {
        SortedMap<String, String> map = new TreeMap<>();
        map.put("data", this.data);
        map.put("key", this.key);

        return CryptoUtil.getSortedData(map);
    }
}



