package com.example.demo;

import java.io.IOException;

public class WkTests {

    public static void main(String[] args) {
        String cmd = "e:/360Downloads/wkhtmltopdf/bin/wkhtmltopdf --quality 75 https://www.nowcoder.com e:/360Downloads/data/wk-/11.pdf";

        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
