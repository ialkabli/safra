package com.iprigner.safra;
import java.time.LocalDate;
/** Host-side pure domain checks; execute with scripts/check-rules.cmd. */
public class RuleChecks {
    static int count;
    static void check(boolean value){if(!value)throw new AssertionError();count++;}
    static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException e){count++;return;}throw new AssertionError("Expected rejection");}
    public static void main(String[] args){
        check(Rules.cost(34900,3)==104700);
        check(Rules.required("  رحلة  ",2,20).equals("رحلة"));
        check(!Rules.overlap(10,20,20,30));
        check(Rules.overlap(10,21,20,30));
        check(Rules.futureDate("2026-11-07",LocalDate.of(2026,11,6)).equals(LocalDate.of(2026,11,7)));
        rejects(()->Rules.futureDate("2026-11-05",LocalDate.of(2026,11,6)));
        rejects(()->Rules.futureDate("2027-11-06",LocalDate.of(2026,11,6)));
        rejects(()->Rules.required(" ",2,20));
        rejects(()->Rules.range(9,1,8));
        rejects(()->Rules.cost(-100,1));
        rejects(()->Rules.interval(10,10,0));
        rejects(()->Rules.interval(10,20,15));
        rejects(()->Rules.interval(10,90000000,0));
        Rules.interval(10,20,0);count++;
        System.out.println(count+" pure rules checks passed");
    }
}
