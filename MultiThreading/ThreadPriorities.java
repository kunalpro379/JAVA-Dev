class ThreadMethods extends Thread {
    @Override
    public void run() {
        System.out.println("Thread started running");
        for (int i = 0; i < 5; i++) {
            String a = "";
            for (int j = 0; j < 5000; j++) {
                a += "a";
            }
            System.out.println(Thread.currentThread().getName() + "- Priority: " + Thread.currentThread().getPriority() + " - Count: " + i);
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            MyThread thread1 = new MyThread("low");
            MyThread thread2 = new MyThread("medium");
            MyThread thread3 = new MyThread("high");
            thread1.setPriority(Thread.MIN_PRIORITY); // 1
            thread2.setPriority(Thread.NORM_PRIORITY); // 5
            thread3.setPriority(Thread.MAX_PRIORITY); // 10
            thread1.start();
            thread2.start();
            thread3.start();
        } catch (Exception e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }
        System.out.println("Thread has finished execution");
    }
}

class MyThread extends Thread {
    public MyThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        System.out.println("Thread started running");
        for (int i = 0; i < 5; i++) {
            String a = "";
            for (int j = 0; j < 5000; j++) {
                a += "a";
            }
            System.out.println(Thread.currentThread().getName() + "- Priority: " + Thread.currentThread().getPriority() + " - Count: " + i);
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }
    }
}
