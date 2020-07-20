package com.hasnicodes.task;

import android.util.Log;

public class AppConstant {

    public static boolean IS_DEVELOPMENT = true;

    public static String dummyLink = "http://www.orimi.com/pdf-test.pdf";
    public static String dummyLinkXSLS = "https://www.cmu.edu/blackboard/files/evaluate/tests-example.xls";
    public  static String[] mimeTypes =
            {"application/pdf", "application/vnd.ms-excel"};

    public static void printError(String mess) {
        if (IS_DEVELOPMENT)
            Log.e("Error ", "is " + mess);
    }


}
