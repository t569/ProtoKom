import java.net.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import java.io.*;
/*
 * Recieve all incoming connections
 * 
 */
public class ProtoServer <T>{
    private final Class<T> modelClass;
    private int PORT;
    private ServerSocket serverSocket;
    private static String name = "localhost";
    private static final ConcurrentHashMap<String, Boolean> ackedClients = new ConcurrentHashMap();
    private static final ConcurrentHashMap<Class<?>, ProtoServer<?>> subServers = new ConcurrentHashMap<>();


    // the database
    private DataBindings<T> database = new DataBindings<>();


    // create a static class instance that handles all the classes
    
    // bind the database to a database structure

    // remember to actually write the DataProvider
    public void bindToDataBase(DataProvider<T> provider)
    {
        database.bindToDataBase(provider);
    }


    
    private ProtoServer(Class<T> modelClass, int port)
    {   
        this.PORT = port;
        this.modelClass = modelClass;
        try 
        {
            this.serverSocket = new ServerSocket(PORT);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        
    }

    // The general behaviour of the Server is to keep listening to a thread of a particular client
    // Until the client themself close the connection. 




    // spool server method
    public static <T> void spoolSubProtoServer(Class<T> myclass, int port)
    {
       if(subServers.containsKey(myclass))
       {
            throw new IllegalStateException("Server instance for type already exists" + myclass.getSimpleName());
       } 

       // if not, we're good, add the instance to the spool pool lmao
       ProtoServer<T> server = new ProtoServer<>(myclass, port);
       subServers.put(myclass, server);

       System.out.println("Successful, server for instance: " + myclass.getSimpleName() + " created.");
    }

    // get server from ConcurrentHashmap method
    @SuppressWarnings("unchecked")
    public static <T> ProtoServer<T> getSubServer(Class<T> myclass)
    {
        return (ProtoServer<T>) subServers.get(myclass);
    }




    // So in essence, while true keep accepting and handling clients
    // TODO: chnage name to start ?
    public static void recieve() throws IOException, ClassNotFoundException
    {
        while (true) {
            Socket client = serverSocket.accept();
            new Thread(() -> handleClient(client)).start();
            
        }
    }
    // accept request from client
        // send a temp connection trigger
        // client recieves this and sends a CONN_REQ
        // server recieves this and sends a CONN_ACK
        // if client sends a CONN_CONF
        // server checks if they are part of server clients
        // if not boot the client


    // run this on a thread Note this should run only once
    public static void handleClient(Socket socket)
    {
        // NETWORK HANDSHAKE
        try(
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());
        )
        {
            // 1. send temp connection trigger
            out.writeObject(new Protocol(
                Status.CONN_INIT_HANDSHAKE,
                new Protocol.Packet(
                    name,                                               /*sender*/
                    socket.getInetAddress().toString(),               /*receiver*/ 
                    "BEGIN HANDSHAKE",                              /*text*/     
                    new Protocol.Packet.MetaData()
                )
            ));
            out.flush();

            // 2. check response type
            Protocol req = (Protocol) in.readObject();
            String clientId = req.getPacket().getSender();
            if(req.getStatus() == Status.CONN_REQ)
            {
                // To do, port logging here too
                System.out.println("Connection request from " + clientId);

                // Search if in hashmap and add it there if it isnt
                if(!ackedClients.containsKey(clientId))
                {
                    ackedClients.put(clientId, Boolean.TRUE);
                }

                // 3. Send the CONN_ACK
                out.writeObject(new Protocol(
                Status.CONN_ACK,
                new Protocol.Packet(
                    name,                                   /*sender*/  
                    clientId,                              /*receiver*/ 
                    "HANDSHAKE ACK",            /*text*/ 
                    new Protocol.Packet.MetaData()
                )
            ));
            out.flush();
            }

            else 
            {
                // 4. HANDLE CONN_CONF
                if(req.getStatus() == Status.CONN_CONF)
                {
                    // search the hashmap if not found boot client
                    if(!ackedClients.containsKey(clientId))
                    {
                        bootClient(socket, out, "Unkown User");
                    }
                    else
                    {
                        // TODO: handle the message
                        //  Note this should be an infinite loop until we have a close request from the client
                        try 
                        {
                            handleClientProtocol(req, socket, in, out);

                            // once it breaks return
                            // socket is closed in the inner function
                            return;
                        } 
                        catch(IOException | ClassNotFoundException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

                else
                {
                    // boot client
                    bootClient(socket, out, "Invalid request");
                }
            }


        }
        catch(ClassNotFoundException | IOException e)
        {
            e.printStackTrace();
        }

    }

    private static void bootClient(Socket socket, ObjectOutputStream out, String reason)
    {
        try
        {
            System.out.println("Booting client" + reason);

            out.writeObject(new Protocol(
                Status.CONN_ACK,
                new Protocol.Packet(
                    name,                                   /*sender*/  
                    socket.getInetAddress().toString(),                              /*receiver*/ 
                    "BOOT: "+ reason,                    /*text*/ 
                    new Protocol.Packet.MetaData()
                )
            ));
            out.flush();
            socket.close();
        }
        catch(IOException e) {}
    }


    // this is when they send a CONN_CONF and were cool with them
    // return a CONN_OK
    public static void handleClientProtocol(Protocol protocol, Socket socket, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException
    {
        // Handles client message and does what he's asked to do
        // TODO: this will be like an entry into another set of utils

        String clientId = protocol.getPacket().getSender();

        // Confirm the full handshake
        Protocol ok = new Protocol(Status.CONN_OK,
                                    new Protocol.Packet(
                                        name,
                                         clientId,
                                        "Connection established",
                                         new Protocol.Packet.MetaData()
                                    )
                                );

        out.writeObject(ok);
        out.flush();


        // while the protocol status is not closed, listen and handle any incoming message
        // main loop
        
        while (true) {
            Protocol msg = (Protocol) in.readObject();
            Status st = msg.getStatus();

            if(st == Status.CONN_DISCONNECT)
            {
                System.out.println("Client " + clientId + "disconnected");
                break;
            }

            Protocol response = handleRequest(msg);
            
            out.writeObject(response);
            out.flush();

        }

        // once connection is broken close the socket
        socket.close();
    }

    public static Protocol handleRequest(Protocol msg)
    {
        
        // TODO: handle GET, POST, UPDATE and DELETE
        // This is finally when we consider MetaData

        
        Optional<Protocol.Packet.MetaData.CommProtocol> opt = msg.getPacket().getMetaData().geCommProtocol();


        // what we will return to the client
        Protocol returnProtocol;


        // if we actually have some metadata
        if(opt.isPresent())
        {
            Protocol.Packet.MetaData.CommProtocol protocol = opt.get();

            // At this point i think its appropriate to begin spooling sub server objects 
            // As well as managing their object pool
            switch(protocol)
            {
                case GET:
                    // do something
                    returnProtocol = handleGet(msg);
                    break;

                case POST:
                    // do something
                    returnProtocol = handlePost(msg);
                    break;

                case UPDATE:
                    // do something
                    returnProtocol = handleUpdate(msg);
                    break;

                case DELETE:
                    // do something
                    returnProtocol = handleDelete(msg);
                    break;

                default:
                    returnProtocol = new Protocol(Status.CONN_OK,
                                    new Protocol.Packet(
                                        name,
                                        msg.getPacket().getSender(),
                                        "Request recieved",
                                        new Protocol.Packet.MetaData()
                                    )
                                );
                    break;
            }

            // we are done with parsing
            return returnProtocol;
        }

        else
        {
            returnProtocol = new Protocol(Status.CONN_OK,
                                    new Protocol.Packet(
                                        name,
                                        msg.getPacket().getSender(),
                                        "Request recieved",
                                        new Protocol.Packet.MetaData()
                                    )
                                );
            return returnProtocol;
        }
        

    }


    // TODO: parse metaData, search the DataBase and return a Protocol response
    public Protocol handleGet(Protocol msg)
    {

    }

    public Protocol handlePost(Protocol msg)
    {

    } 

    public Protocol handleUpdate(Protocol msg)
    {

    }

    public Protocol handleDelete(Protocol msg)
    {

    }
}


// The whole idea is that there is only one server
// However, that server has subservers that handle individual ORMS
// so the methods not marked as static are for them
// the methods marked as static are for the main server
/*
     * Example Syntax
     * 
     *  public static void main(String[] args) throws IOException {
            SessionFactory sf = SessionFactoryProvider.provideSessionFactory();

            ProtoServer<User> userServer = ProtoServer.spoolSubProtoServer(User.class, 9001);
            userServer.bindToDatabase(new OrmDataProvider<>(sf, User.class));

            ProtoServer<Product> productServer = ProtoServer.spoolSubProtoServer(Product.class, 9002);
            productServer.bindToDatabase(new ListDataProvider<>(new ArrayList<>()));

            new Thread(userServer::start).start();
            new Thread(productServer::start).start();
    }
     */

   
