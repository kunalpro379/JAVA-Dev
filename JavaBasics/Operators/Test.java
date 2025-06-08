public class Test{
    public static void main(String []args){
        int a=1;
        System.out.println(a);
        System.out.println(Integer.toString(a));
        String name="Kunal";
        System.out.println(name);
        System.out.println(name.length());
        System.out.println(toInteger(name)); 
        for(int i=1;i<10;i+=3){
            System.out.println(i*i*8);
        }   
        int x=1;
        while(++x<10){
            System.out.println(x);
            x/=1; 
        }
    }
    public static int[] toInteger(String str){
        try{
            int[] arr = new int[str.length()];
            for(int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                System.out.println(c + ": " + (int)c);
                arr[i]=(int)c;
            }return arr;
        }catch(NumberFormatException e){
            System.out.println(e);
            return null;
        }
    }

}