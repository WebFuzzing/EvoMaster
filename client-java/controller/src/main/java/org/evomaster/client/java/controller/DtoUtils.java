package org.evomaster.client.java.controller;

import org.evomaster.client.java.controller.api.dto.SutInfoDto;

/**
 * util class for handling dto
 */
public class DtoUtils {

    /**
     *
     * @param outputFormat
     * @return whether the specified format is Java
     */
    public static boolean isJava(SutInfoDto.OutputFormat outputFormat){
        return outputFormat.name().startsWith("JAVA");
    }

    /**
     *
     * @param outputFormat
     * @return whether the specified format is Kotlin
     */
    public static boolean isKotlin(SutInfoDto.OutputFormat outputFormat){
        return outputFormat.name().startsWith("KOTLIN");
    }

    /**
     *
     * @param outputFormat
     * @return whether the specified format is Java or Kotlin
     */
    public static boolean isJavaOrKotlin(SutInfoDto.OutputFormat outputFormat){
        return isJava(outputFormat) || isKotlin(outputFormat);
    }

}
