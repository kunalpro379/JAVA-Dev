import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
public class Server{
    public Consumer<Socket>getConsumer(){
        return ()->{try(){}catch(IOException ex){}}
    }
}