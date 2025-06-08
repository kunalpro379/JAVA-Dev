public class Test {
  public static void main(String[] args) {
    System.out.println("Program started");
    main1(args);
    main2(args);
    main3(args);
    String result = main3(args);
    System.out.println(result);

  }
    public static String main3(String[] args) {
        return "String";
    }
    public static void main1(String[] args) {
      System.out.println("Hello World"); 
    }
    public static void main2(String[] args) {
      System.out.println("Hello World"); 
    }

}

//Static meaning we can call it without creating an object of Test