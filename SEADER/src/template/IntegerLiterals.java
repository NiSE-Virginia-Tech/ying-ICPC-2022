package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IntegerLiterals {
    private List<String> literals = null;
    public IntegerLiterals(String ... list) {
        literals = new ArrayList<String>();
        for (int i = 0; i < list.length; i++) {
            literals.add(list[i]);
        }
    }
    public String getAString() {
        return literals.get(new Random().nextInt(literals.size()));
    }
}
