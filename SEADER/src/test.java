import template.StringLiterals;

import javax.crypto.Cipher;

public class test {

    public void main(String[] args) throws Exception{
        StringLiterals incorrects = new StringLiterals("DES", "MD5");
        StringLiterals corrects = new StringLiterals("b", "a");
        Cipher c = Cipher.getInstance(incorrects.getAString());
        int a = 1;
    }

}