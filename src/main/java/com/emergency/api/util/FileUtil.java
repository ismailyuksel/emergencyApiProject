package com.emergency.api.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;



public class FileUtil {

    private static String readAll(Reader rd) throws IOException {
      StringBuilder sb = new StringBuilder();
      int cp;
      while ((cp = rd.read()) != -1) {
        sb.append((char) cp);
      }
      return sb.toString();
    }

    public static String readStringFromFile(String fileName) throws IOException {
        Resource resource = new ClassPathResource(fileName);
        InputStream input = resource.getInputStream();
          try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
            return readAll(rd);
          } finally {
        	 input.close();
          }
    }
}
