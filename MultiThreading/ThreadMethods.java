 class ThreadMethods extends Thread{
    @Override
     public void run(){
        System.out.println("Thread is running");
        for(int i=1;i<=5;i++){
            System.out.println("Count: " + i);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }
    }
    public static void main(String[] args){
        ThreadMethods thread = new ThreadMethods();
        try {
            thread.start(); // Start the thread
            thread.join();
            System.out.println("hey kunal");

        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }
        System.out.println("Thread has finished execution");
    }
 }