package models;

import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-03
 * Time: 4:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class Mallet {

    private static Mallet ref;

    private Mallet() {
    }

    public String infer(String[] tokens){
        return "";
    }

    public static synchronized Mallet instance() {
        if(ref == null){
            ref = new Mallet();
        }
        return ref;
    }
}
