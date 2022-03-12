package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StringLiterals {
    private List<String> literals = new ArrayList<String>();
    public StringLiterals(String ... list) {
        for (int i = 0; i < list.length; i++) {
            literals.add(list[i]);
        }
    }
    public String getAString() {
        int i = new Random().nextInt(literals.size());
        return literals.get(i);
    }
}

