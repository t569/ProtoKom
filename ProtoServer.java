import java.net.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import java.io.*;
import java.lang.classfile.ClassFile.Option;
/*
 * Recieve all incoming connections
 * 
 */
public class ProtoServer{
    private int PORT;
    private ServerSocket serverSocket;
    private static String name = "localhost";
    private static final ConcurrentHashMap<String, Boolean> ackedClients = new ConcurrentHashMap();
    private final ConcurrentHashMap<Class<?>, QueryHandler<?>> queries = new ConcurrentHashMap<>();

    /*
     * MODIFICATION: 
     * The modification Creates a new sub class called BindHandler that handles different data and is what is actually bound to the database
     *
     * 
     */

     private class QueryHandler<T>
     {
        private DataBindings<T> query = new DataBindings<>();
        public QueryHandler(Class <T> modelClass)
        {

        }

        // bind the bindhandler query object to a Data provider
        public void bindToDataBase(DataProvider<T> provider)
        {
            query.bindToDataBase(provider);
        }
     }
    
    // remember to actually write the DataProvider
    // public void bindToDataBase(DataProvider<T> provider)
    // {
    //     database.bindToDataBase(provider);
    // }


    
    public ProtoServer(int port)
    {   
        this.PORT = port;
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
    public <T> void spoolQuery(Class<T> myclass, DataProvider<T> provider)
    {
       if(queries.containsKey(myclass))
       {
            throw new IllegalStateException("Server instance for type already exists" + myclass.getSimpleName());
       } 

       // if not, we're good, add the instance to the spool pool lmao
       /*
        * Bind it to the provider first
        * Then add it to the hashmap
        */
       QueryHandler<T> query = new QueryHandler<>(myclass);
       query.bindToDataBase(provider);
       queries.put(myclass, query);

       System.out.println("Successful, server for instance: " + myclass.getSimpleName() + " created.");
    }

    // get server from ConcurrentHashmap method
    @SuppressWarnings("unchecked")
    public <T> QueryHandler<T> getQuery(Class<T> myclass)
    {
        return (QueryHandler<T>) queries.get(myclass);
    }

    public boolean matchesTypeKey(String typekey, Class<?> modelClass)
    {
        return modelClass.getSimpleName().equalsIgnoreCase(typekey);
    }

    public <T> QueryHandler<T> findQueryHandlerByTypeKey(String typekey, ConcurrentHashMap<Class<?>, QueryHandler<?>>queries)
    {
        for(Class<?> modelClass: queries.keySet())
        {
            // this makes sure we ignore case sensitivity e.g. User and USER works
            // TODO: check if this is a bad idea

            // TODO: handle the casting ambiguities
            if(modelClass.getSimpleName().equalsIgnoreCase(typekey))
            {
                return (QueryHandler<T>) queries.get(modelClass);
            }
        }

        // nothing found
        return null;
    }



    // So in essence, while true keep accepting and handling clients
    // TODO: chnage name to start ?
    // TODO: create a thread pool to prevent DDOS attacks, check out Nnnanna's message
    public void recieve() throws IOException, ClassNotFoundException
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
    public void handleClient(Socket socket)
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

    private void bootClient(Socket socket, ObjectOutputStream out, String reason)
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
    public void handleClientProtocol(Protocol protocol, Socket socket, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException
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

    public Protocol handleRequest(Protocol msg)
    {
        
        // TODO: handle GET, POST, UPDATE and DELETE
        // This is finally when we consider MetaData

        
        Optional<Protocol.Packet.MetaData.CommProtocol> opt = msg.getPacket().getMetaData().geCommProtocol();


        // what we will return to the client
        Protocol returnProtocol;


        // if we actually have some metadata
        if(opt.isPresent())
        {
            // get the protocol
            Protocol.Packet.MetaData.CommProtocol protocol = opt.get();
            Object payload = msg.getPacket().getMetaData().getPayload(); 

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

    // TODO:
            // search the queries for the type of the payload
            // pass into into the handleXyz(...) method arguments
    // TODO: parse metaData, search the DataBase and return a Protocol response


    // public <T> Protocol handleGet(Protocol msg)
    // {
    //     String clientID = msg.getPacket().getReciever();
    //     Optional<Object> payload = msg.getPacket().getMetaData().getPayload();


    //     if(!payload.isPresent())
    //     {
    //         return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "GET failed: No payload", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
    //     }

    //     // this works for getting a particular Object
    //     Object object_id_to_get = payload.get();

    //     // TODO: handle the cast failure
    //     QueryHandler<T> handler = getQuery((Class<T>) object_id_to_get.getClass());

    //     if(handler == null)
    //     {
    //         return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "GET failed: No handler for type", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));  
    //     }

    //     try 
    //    {
    //         T object_to_get = handler.query.get(object_id_to_get);
    //         return new Protocol(Status.CONN_OK, new Protocol.Packet(name,clientID, "GET: success", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK, object_to_get)));
    //    }
    //    catch(Exception e)
    //    {
    //         return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "GET failed: " + e.getMessage(),new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
    //    }
    // }


    // Note: this implementation works with Object id objects, so searching by id or a field
    public <T> Protocol handleGet(Protocol msg)
    {
        String clientID = msg.getPacket().getReciever();

        // payload is just and Object. In this case a String or an id
        Optional<Object> payload = msg.getPacket().getMetaData().getPayload();

        // get the actual key that we're searching
        Optional<String> typekey = msg.getPacket().getMetaData().getKey();

        // check if the key exists and error if it does not;
        // Note: key should only exist for GET and DELETE methods

        QueryHandler<T> handler;
        if(typekey.isPresent())
        {
            // now search query to see if we get a match on type key
             handler = findQueryHandlerByTypeKey(typekey.get(), queries);

            // we dont have a handler for this type specified
            if(handler == null)
            {
                return new Protocol(Status.CONN_OK,new Protocol.Packet(name, clientID, "GET failed: No handler for type", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
            }

            // now check if we actually have a payload
            if(!payload.isPresent())
            {
                return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "GET failed: No payload", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
            }

            Object object_id_to_get = payload.get();
            try 
            {
                T object_to_get = handler.query.get(object_id_to_get);
                return new Protocol(Status.CONN_OK, new Protocol.Packet(name,clientID, "GET: success", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK, object_to_get)));

            }
            catch(Exception e)
            {
                return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "GET failed: " + e.getMessage(),new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
            }
        }

        else
        {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "GET failed: No supplied type Key",new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR))); 
        }

        

    }
    public <T> Protocol handlePost(Protocol msg)
    {
        String clientID = msg.getPacket().getReciever();
        Optional<Object> payload = msg.getPacket().getMetaData().getPayload();


        if(!payload.isPresent())
        {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(
            name, clientID, "GET failed: No ID provided", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
        }

        Object object_to_post = payload.get();


       // TODO: handle when this cast fails
       QueryHandler<T> handler = getQuery((Class<T>) object_to_post.getClass());

       if(handler == null)
       {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "POST failed: No handler for type", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
       }


       try 
       {
            handler.query.post((T) object_to_post);
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name,clientID, "POST: success", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK)));
       }
       catch(Exception e)
       {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "POST failed: " + e.getMessage(),new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
       }
    } 

    public <T> Protocol handleUpdate(Protocol msg)
    {
        String clientID = msg.getPacket().getReciever();
        Optional<Object> payload = msg.getPacket().getMetaData().getPayload();

        if(!payload.isPresent())
        {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "UPDATE failed: No payload", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
        }

        // TODO: handle when type cast fails
        Object object_to_update = payload.get();
        QueryHandler<T> handler = getQuery((Class<T>) object_to_update.getClass());

        if(handler == null){
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "UPDATE failed: No handler for type", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
        }

        try
        {
            handler.query.update((T) object_to_update);
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "UPDATE success", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK)));
        }
        catch(Exception e)
        {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "UPDATE failed: " + e.getMessage(), new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
        }

    }

    public <T> Protocol handleDelete(Protocol msg)
    {
        String clientID = msg.getPacket().getReciever();
        Optional<Object> payload = msg.getPacket().getMetaData().getPayload();

        if(!payload.isPresent())
        {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "DELETE failed: No payload", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
        }

        Object object_to_delete = payload.get();
        QueryHandler<T> handler = getQuery((Class<T>) object_to_delete.getClass());

        if(handler == null)
        {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "DELETE failed: No handler for type", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
        }

        try 
        {   
            // TODO: handle when type casting fails
            handler.query.delete((T) object_to_delete);
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "DELETE success", new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK)));
        }
        catch(Exception e)
        {
            return new Protocol(Status.CONN_OK, new Protocol.Packet(name, clientID, "DELETE failed: " + e.getMessage(), new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)));
        }
    }
}

// TODO: rework get and delete, something wrong with them

// we are supposed to use an id or index to get objects of type T

// and delete should have both delete for id or index and for object of type T






// The whole idea is that there is only one server
// However, that server has subservers that handle individual ORMS
// so the methods not marked as static are for them
// the methods marked as static are for the main server
/*

     */

   
