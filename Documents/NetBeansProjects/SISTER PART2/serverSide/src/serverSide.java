/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.esotericsoftware.kryonet.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrian
 */
public class serverSide extends Listener 
{
    //object Server
    static Server server, server1;
    static Client client;
    //Server Port
    static int udpPort=45555,tcpPort=45555;
    
    static int udpServer=44444,tcpServer=44444;
    
    static String namaTabel="";

    static String key="";
    
    static String value="";
    
    
    static int startToken=0;
    static int endToken=0;
    
    static int counter = 0;
    
    static boolean create=false;
    static boolean insert=false;
    static boolean display=false;
    
    static Connection requestConn;
    
    static int maxSize = 0;
    static String IPmaxSize = "";
    static String Mess;
    static int statusServer=0;
    static ArrayList<String> ip=new ArrayList<>();
    //representasi database dengan struktur HashMap<NamaTabel, Isi tabel>
    static HashMap<String,HashMap<String, ArrayList<String>>> database=new HashMap<>();
    public static void main(String[] args) throws Exception
    {
        //  HIDUPIN SERVER BUAT DENGERIN CLIENT
        
        //server inisialisasi
        server = new Server();

        //register packet class
        //server.getKryo().register(PacketMessage.class);
        
        //binding port ke port client
        server.bind(tcpPort, udpPort);
        
        //start server
        server.start();
        
        //tambahin listener
        server.addListener(new serverSide());
        
        
        
        System.out.println("Server ngedengerin client");
        
        //  HIDUPIN SERVER BUAT DENGERIN SESAMA SERVER
   
        //server inisialisasi
        server1 = new Server();

        //register packet class
        //server1.getKryo().register(PacketMessage.class);
        
        //server1.getKryo().register(HashMap.class);
        
        //binding port ke port server
        server1.bind(tcpServer, udpServer);
        
        //start server
        server1.start();

        //tambahin listener
        server1.addListener(new serverSide());
        
        System.out.println("Server ngedengerin sesama server");
        
        //  HIDUPIN CLIENT
        
        client = new Client();

        //mendaftarkan kelas PacketMessage ke dalam jaringan agar mau terkirim di jaringan
        client.getKryo().register(PacketMessage.class);
        client.getKryo().register(HashMap.class);
        client.getKryo().register(ArrayList.class);
        client.getKryo().register(Tabel.class);
        server.getKryo().register(PacketMessage.class);
        server.getKryo().register(HashMap.class);
        server.getKryo().register(ArrayList.class);
        server.getKryo().register(Tabel.class);
       
        server1.getKryo().register(PacketMessage.class);
        server1.getKryo().register(HashMap.class);
        server1.getKryo().register(ArrayList.class);
        server1.getKryo().register(Tabel.class);
        
        //client start
        client.start();

         
        ArrayList<Client> allClient=new ArrayList<>();
        ip.add("192.168.43.206");
        ip.add("192.168.43.43");

        
        
        
        //Mencoba melakukan koneksi dengan server jika gagal atau server masih belum nyala
        //akan menampilkan pesan
        for (int i = 0; i < ip.size(); i++)
        {
                //  HIDUPIN CLIENT

                Client clientD = new Client();

                //mendaftarkan kelas PacketMessage ke dalam jaringan agar mau terkirim di jaringan
                clientD.getKryo().register(PacketMessage.class);
                clientD.getKryo().register(Tabel.class);
                clientD.getKryo().register(HashMap.class);
                
                clientD.addListener(new serverSide());   
                 //client start
                new Thread(clientD).start();
                allClient.add(clientD);
                try 
                {              
                    allClient.get(i).connect(5000, ip.get(i), tcpServer, udpServer);
                    // minta data
                       //Buat sebuah paket message
                    counter++;
                    PacketMessage packetMessage = new PacketMessage();

                    //Buat sebuah pesannya
                    packetMessage.message = "size";
                    
                    //Kirim pesannya
                    allClient.get(i).sendTCP(packetMessage);
                    
                
                }
                catch (Exception e) 
                {
                    System.out.println("Server "+ip.get(i)+" belum siap");
                }
            
        }
        //while (counter != 1) {
             
        //}
        System.out.print(counter);
        
        while(counter!=0)
        {
            Thread.sleep(100);
        }
       if (maxSize == 0)
        {
            startToken=0;
            endToken=(int) Math.pow(2, 31);
        }
         else
        {
            
            allClient.get(0).close();
             // minta data
             //Buat sebuah paket message
             PacketMessage packetMessage = new PacketMessage();

             //Buat sebuah pesannya
             packetMessage.message = "get";

             //Kirim pesannya
             client.connect(5000, IPmaxSize, tcpServer, udpServer);
             client.sendTCP(packetMessage);


            client.addListener(new serverSide());


        }

       client.addListener(new serverSide());
       while(true)
       {
           if(create)
           {
               for(int i=0;i<allClient.size();i++)
               {
                     //Buat sebuah paket message
                    PacketMessage packetMessage = new PacketMessage();

                    //Buat sebuah pesannya
                    packetMessage.message = "create server "+namaTabel;
                    Client send=new Client();
                    
                    if(allClient.get(i).isConnected())
                    {    
                    
                    //Kirim pesannya
                    allClient.get(i).sendTCP(packetMessage);
                    //allClient.get(i).addListener(new serverSide());
                    }
               }
               create=false;
           }
           if(insert)
           {
               for(int i=0;i<allClient.size();i++)
               {
                     //Buat sebuah paket message
                    PacketMessage packetMessage = new PacketMessage();

                    //Buat sebuah pesannya
                    packetMessage.message = "insert server "+namaTabel+" "+key+" "+value;
                    Client send=new Client();
                    
                    if(allClient.get(i).isConnected())
                    {    
                    
                    //Kirim pesannya
                    allClient.get(i).sendTCP(packetMessage);
                    //allClient.get(i).addListener(new serverSide());
                    }
               }
               System.out.println("debug");
               insert=false;
           }
           if(display)
           {
               for(int i=0;i<allClient.size();i++)
               {
                     //Buat sebuah paket message
                    PacketMessage packetMessage = new PacketMessage();

                    //Buat sebuah pesannya
                    packetMessage.message = "display server "+namaTabel;

                    if(allClient.get(i).isConnected())
                    {    
                    allClient.get(i).getKryo().register(Tabel.class);
                    //Kirim pesannya
                    allClient.get(i).sendTCP(packetMessage);
                    //allClient.get(i).addListener(new serverSide());
                    counter++;
                    System.out.println(counter);
                    }
               }
               
               Thread.sleep(1000);
               while (counter > 0)
               {
                   Thread.sleep(100);
                   //System.out.println("ampas emang "+counter);
               }
               //System.out.println("debug");
               display=false;
               PacketMessage packetMessage = new PacketMessage();
               packetMessage.message = Mess;
               requestConn.sendTCP(packetMessage);
           }
           Thread.sleep(100);
       } 
       
        
        
        
    }
    
    //Ini dijalankan kalo dapet koneksi
    public void connected(Connection c)
    {
        System.out.println("Received a connection from "+c.getRemoteAddressTCP().getHostName());
        //Kirim pesannya
        //c.sendTCP(packetMessage);
    }
    
    //Ini dijalankan saat kita menerima paket
    public void received (Connection c, Object p)
    {
        if(p instanceof HashMap)
        {
             database=(HashMap<String, HashMap<String, ArrayList<String>>>) p;
            System.out.println("database sudah diterima dari"+c.getRemoteAddressTCP().getAddress().getHostAddress());
            ArrayList<String> listTabel=new ArrayList<>();
            ArrayList<String> isiTabel=new ArrayList<>();
            for (Map.Entry<String,HashMap<String, ArrayList<String>>> entry : database.entrySet()) {

                for (Map.Entry<String, ArrayList<String>> entry1 : entry.getValue().entrySet()) {
                    int code = Math.abs(entry1.getKey().hashCode());
                    if (!(code >= startToken && code < endToken))
                    {
                        listTabel.add(entry.getKey());
                        isiTabel.add(entry1.getKey());
                        //databaseKirim.get(entry.getKey()).remove(entry1.getKey());
                    }

                }
            }
            for(int i=0;i<listTabel.size();i++)
            {
                database.get(listTabel.get(i)).remove(isiTabel.get(i));
            }

        }
        if (p instanceof ArrayList)
        {
            System.out.println("Tahap4");
            ArrayList<String> dummy = (ArrayList<String>) p;
            if (dummy.get(0).equals("mess"))
            Mess = Mess + dummy.get(1);
            //Tabel dummy=(Tabel) p;
            //Map<String, ArrayList<String>> tabelDummy = dummy.Isi;
            //for (Map.Entry<String, ArrayList<String>> entry : tabelDummy.entrySet()) 
                //{
                    //menggunakan sizeLIst bertujuan agar dapat mengambil data dengan timestamp terbaru
                    //int sizeList=entry.getValue().size();
                    //Mess = Mess + entry.getKey().hashCode() + " " + entry.getValue().get(sizeList-2) + " " + entry.getValue().get(sizeList-1) + "\n" ;
                //}
            counter--;
            System.out.println("Tahap5");
        }
        
        if(p instanceof PacketMessage)
		{
			PacketMessage packet = (PacketMessage) p;
			if(packet.message != null) {
                            System.out.println("Received : "+packet.message+" "+c.getRemoteAddressTCP().getHostString());
                        
			//buat variable buat cek
			boolean cek=false;
						
                        //Pesan di pecah berdasarkan spasi 
                        String[] parse=packet.message.split(" ");
                        
                        //Pesan yang terakhir di ambil
                        String temp=parse[parse.length-1];
                        
                        //titik koma terakhir di hilangin
                        if(temp.charAt(temp.length()-1)==';')
                        {
                        	cek=true;
                        	
                        	//di update data
                        	String[] tempString=parse[parse.length-1].split(";");
                        	parse[parse.length-1]=tempString[0];
                        }
                        
                        //proses cek input an bakal di proses kayak gimana
                        if(parse[0].toLowerCase().equals("create") && parse[1].toLowerCase().equals("table") && parse.length==3 && cek)
                        {
                            //requestConn = c;
                            namaTabel=parse[2];
                            if (database.get(namaTabel)!= null)
                            {
                                   //Buat sebuah paket message
                                   PacketMessage packetMessage = new PacketMessage();

                                   //Buat sebuah pesannya
                                   packetMessage.message = "Table "+namaTabel+" sudah ada";

                                   //Kirim pesannya
                                   c.sendTCP(packetMessage);
                            }
                            else
                            {
                                create = true;
                                PacketMessage packetMessage = new PacketMessage();
                                packetMessage.message = "Tabel berhasil dibuat";
                                c.sendTCP(packetMessage);
                            }
                            
                            
                        }
                        else if (parse[0].toLowerCase().equals("create") && parse[1].toLowerCase().equals("server") && parse.length==3)
                        {
                            HashMap<String, ArrayList<String>> dummy=new HashMap<>();
                            database.put(parse[2],dummy);
                        }
                        else if(parse[0].toLowerCase().equals("insert") && parse[1].toLowerCase().equals("server") && parse.length==5)
                        {
                            if(Math.abs(parse[3].hashCode()) >= startToken && Math.abs(parse[3].hashCode()) < endToken) {
                                insertTable(parse[2], parse[3], parse[4], c);
                                System.out.println(parse[4]);
                            }
                        }
                        else if(parse[0].toLowerCase().equals("insert") && parse.length==4 && cek)
                        {
                            requestConn = c;
                            namaTabel=parse[1];
                            if (database.get(namaTabel)== null)
                            {
                                   //Buat sebuah paket message
                                   PacketMessage packetMessage = new PacketMessage();

                                   //Buat sebuah pesannya
                                   packetMessage.message = "Table "+namaTabel+" tidak ada";

                                   //Kirim pesannya
                                   c.sendTCP(packetMessage);
                            }
                            else
                            {
                                insert = true;
                                key = parse[2];
                                value = parse[3];
                            }
                        }
                        else if(parse[0].toLowerCase().equals("display") && parse.length==2 && cek)
                        {
                            Mess = "\n";
                            display = true;
                        }
                        else if(parse[0].toLowerCase().equals("display") && parse[1].toLowerCase().equals("server") && parse.length==3)
                        {
                            System.out.println("Tahap2");
                            String temps = "";
                            Map<String, ArrayList<String>> tabelDummy = database.get(parse[2]);
                            for (Map.Entry<String, ArrayList<String>> entry : tabelDummy.entrySet()) 
                            {
                                //menggunakan sizeLIst bertujuan agar dapat mengambil data dengan timestamp terbaru
                                int sizeList=entry.getValue().size();
                                temps = temps + entry.getKey() + " " + entry.getValue().get(sizeList-2) + " " + entry.getValue().get(sizeList-1) + " \n " ;
                            }
                            packet.message="mess "+temps;
                            c.sendTCP(packet);
                            System.out.println("Tahap3");
                        }
                        else if (parse[0].equals("mess"))
                        {
                            for (int i = 1; i < parse.length; i++)
                            {
                                Mess = Mess + parse[i]+ " ";
                            }
                            //System.out.println("Tahap6 + "+counter);
                            counter--;
                        }
                        //digunakan untuk mengecek semua timestamp kesimpen atau ga
                        else if(parse[0].toLowerCase().equals("display") && parse[1].toLowerCase().equals("all") && parse.length==3 && cek)
                        {
                            displayAllTable(parse[2], c);
                        }
                        else if(parse[0].equals("size") && parse.length==1)
                        {
                            packet.message="size "+(endToken-startToken)+" "+endToken;
                            c.sendTCP(packet);
                        }
                        else if(parse[0].equals("size") && parse.length==3)
                        {
                            
                            if(maxSize<Integer.parseInt(parse[1]))
                            {
                                maxSize=Integer.parseInt(parse[1]);
                                IPmaxSize=c.getRemoteAddressTCP().getAddress().getHostAddress();
                                endToken=Integer.parseInt(parse[2]);
                                startToken=Integer.parseInt(parse[1])/2;
                            }
                            System.out.println(counter);
                            counter--;
                            

                            
                        }
                        else if(parse[0].equals("get") && parse.length==1)
                        {
                            endToken=(endToken-startToken)/2; 
                            ArrayList<String> listTabel=new ArrayList<>();
                            ArrayList<String> isiTabel=new ArrayList<>();
                            HashMap<String,HashMap<String, ArrayList<String>>> databaseKirim=(HashMap<String,HashMap<String, ArrayList<String>>>) database.clone();
                            for (Map.Entry<String,HashMap<String, ArrayList<String>>> entry : databaseKirim.entrySet()) {
                               
                                for (Map.Entry<String, ArrayList<String>> entry1 : entry.getValue().entrySet()) {
                                    int code = Math.abs(entry1.getKey().hashCode());
                                    if (!(code >= startToken && code < endToken))
                                    {
                                        listTabel.add(entry.getKey());
                                        isiTabel.add(entry1.getKey());
                                        //databaseKirim.get(entry.getKey()).remove(entry1.getKey());
                                    }

                                }
                            }
                            
                            c.sendTCP(databaseKirim);
                            for(int i=0;i<listTabel.size();i++)
                            {
                                database.get(listTabel.get(i)).remove(isiTabel.get(i));
                            }
                        }
                        //kalau semua command tidak tepat
                        else
                        {
                            packet.message="maaf command tidak ada atau tidak valid "+parse.length+" "+parse[0]+parse[1];
                            c.sendTCP(packet);
                        }
                        } else {
                            packet.message="maaf command tidak ada atau tidak valid";
                            c.sendTCP(packet);
                        }
		}
    }
    
    //Ini dijalankan kalo clientnya disconnect
    public void disconnected(Connection c)
    {
        System.out.println("A client disconnected!");
    }
    
    //fungsi insert ke database
    public void insertTable(String namaTabel,String key,String value,Connection client)
    {
         //cek apakah tabel sudah ada atau belum
        if (database.get(namaTabel)== null)
        {
             //Buat sebuah paket message
            PacketMessage packetMessage = new PacketMessage();

            //Buat sebuah pesannya
            packetMessage.message = "Table "+namaTabel+" tidak ada";

            //Kirim pesannya
            client.sendTCP(packetMessage);
        }
        else
        {
           //ambil table terlebih dahulu. agar tabel yang akan di masukkan tidak ketimpa
           HashMap<String, ArrayList<String>> tabelDummy=database.get(namaTabel);
           
           //variable dummy untuk dimasukkan ke dalam tabel
           ArrayList<String> valueDummy=new ArrayList<>();
           
           java.util.Date oldTimeStamp= new java.util.Date();
           
           //cek apakah key nya sudah ada atau belum
           if(tabelDummy.get(key)!=null)
           {
               valueDummy=tabelDummy.get(key);
           }
           
           //tambah ke table dummy
           valueDummy.add(value);
           valueDummy.add(new Timestamp(oldTimeStamp.getTime()) + "");
           tabelDummy.put(key,valueDummy);
           database.put(namaTabel, tabelDummy);
           
           //Buat sebuah paket message
           PacketMessage packetMessage = new PacketMessage();

           //Buat sebuah pesannya
           packetMessage.message = "Data berhasil di masukkan ";

           //Kirim pesannya
           requestConn.sendTCP(packetMessage);
        }
    }
    
    //fungsi menampilkan isi tabel dari database
    public void displayTable(String namaTabel, Connection client)
    {
         //cek apakah tabel sudah ada atau belum
        if (database.get(namaTabel)== null)
        {
             //Buat sebuah paket message
            PacketMessage packetMessage = new PacketMessage();

            //Buat sebuah pesannya
            packetMessage.message = "Table "+namaTabel+" tidak ada";

            //Kirim pesannya
            client.sendTCP(packetMessage);
        }
        else
        {
           //HashMap<String, ArrayList<String>> tabelDummy=database.get(namaTabel);
            
           //text yang digunakan untuk menggabungkan string dari table
           Mess = "\n";
           ArrayList<Client> allClient = new ArrayList<>();
           for (int i = 0; i < ip.size(); i++)
           {
                //  HIDUPIN CLIENT

                Client clientD = new Client();

                //mendaftarkan kelas PacketMessage ke dalam jaringan agar mau terkirim di jaringan
                clientD.getKryo().register(PacketMessage.class);
                clientD.getKryo().register(Tabel.class);
                clientD.getKryo().register(HashMap.class);
                
                clientD.addListener(new serverSide());   
                //client start
                new Thread(clientD).start();
                allClient.add(clientD);
                try 
                {              
                    //allClient.get(i).connect(5000, ip.get(i), tcpServer, udpServer);
                    // minta data
                       //Buat sebuah paket message
                    counter++;
                    PacketMessage packetMessage = new PacketMessage();

                    //Buat sebuah pesannya
                    packetMessage.message = "display server "+namaTabel;
                    
                    //Kirim pesannya
                    allClient.get(i).sendTCP(packetMessage);
                }
                catch (Exception e) 
                {
                    System.out.println("Server "+ip.get(i)+" belum siap");
                }
        }
        //System.out.print(counter);
        while(counter!=0)
        {
               try {
                   Thread.sleep(100);
               } catch (InterruptedException ex) {
                   Logger.getLogger(serverSide.class.getName()).log(Level.SEVERE, null, ex);
               }
        }
          
           //Buat sebuah paket message
            PacketMessage packetMessage = new PacketMessage();

            //Buat sebuah pesannya
            packetMessage.message = Mess;

            //Kirim pesannya
            client.sendTCP(packetMessage);
      
        }
    }
    
    //fungsi menampilkan isi tabel dari database
    public void displayAllTable(String namaTabel, Connection client)
    {
         //cek apakah tabel sudah ada atau belum
        if (database.get(namaTabel)== null)
        {
             //Buat sebuah paket message
            PacketMessage packetMessage = new PacketMessage();

            //Buat sebuah pesannya
            packetMessage.message = "Table "+namaTabel+" tidak ada";

            //Kirim pesannya
            client.sendTCP(packetMessage);
        }
        else
        {
           //HashMap<String, ArrayList<String>> tabelDummy=database.get(namaTabel);
            
           Map<String, ArrayList<String>> tabelDummy = database.get(namaTabel);
           
           String Mess = "\n";
           
           // untuk setiap kolom di masukin ke dalam string yang buat ngirim
           for (Map.Entry<String, ArrayList<String>> entry : tabelDummy.entrySet()) 
           {
               String value="";
               int counter=1;
               for(String valueString: entry.getValue())
               {
                  value=value+" "+valueString;
               }

               Mess = Mess + entry.getKey() +value+"\n" ;
           }
           
           //Buat sebuah paket message
            PacketMessage packetMessage = new PacketMessage();

            //Buat sebuah pesannya
            packetMessage.message = Mess;

            //Kirim pesannya
            client.sendTCP(packetMessage);
      
        }
    }
    
    //fungsi create ke database
    public static void createTable(String namaTabel,Connection client)
    {
        //cek apakah tabel sudah ada atau belum
        if (database.get(namaTabel)!= null)
        {
             //Buat sebuah paket message
            PacketMessage packetMessage = new PacketMessage();

            //Buat sebuah pesannya
            packetMessage.message = "Table "+namaTabel+"  ada";

            //Kirim pesannya
            client.sendTCP(packetMessage);
        }
        else
        {
            
           ArrayList<Client> allClient = new ArrayList<>();
           for (int i = 0; i < ip.size(); i++)
           {
                //  HIDUPIN CLIENT

                Client clientD = new Client();

                //mendaftarkan kelas PacketMessage ke dalam jaringan agar mau terkirim di jaringan
                clientD.getKryo().register(PacketMessage.class);
                clientD.getKryo().register(Tabel.class);
                clientD.getKryo().register(HashMap.class);
                
                clientD.addListener(new serverSide());   
                //client start
                new Thread(clientD).start();
                allClient.add(clientD);
                try 
                {              
                    allClient.get(i).connect(5000, ip.get(i), tcpServer, udpServer);
                    // minta data
                       //Buat sebuah paket message
                    counter++;
                    PacketMessage packetMessage = new PacketMessage();

                    //Buat sebuah pesannya
                    packetMessage.message = "create server "+namaTabel;
                    
                    //Kirim pesannya
                    allClient.get(i).sendTCP(packetMessage);
                }
                catch (Exception e) 
                {
                    System.out.println("Server "+ip.get(i)+" belum siap "+e.getMessage());
                }
        }
            
             //Buat sebuah paket message
            PacketMessage packetMessage = new PacketMessage();

            //Buat sebuah pesannya
            packetMessage.message = "Table "+namaTabel+" sudah di buat";

            //Kirim pesannya
            client.sendTCP(packetMessage);

        }
    }
    
    
}

class Tabel {
    
public HashMap<String, ArrayList<String>> Isi = new HashMap();
    
}
