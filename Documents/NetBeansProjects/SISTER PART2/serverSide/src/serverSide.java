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

/**
 *
 * @author andrian
 */
public class serverSide extends Listener 
{
    //object Server
    static Server server;
    static Client client;
    //Server Port
    static int udpPort=45555,tcpPort=45555;
    
    static int udpServer=44444,tcpServer=44444;
    
    static int statusServer=0;
    //representasi database dengan struktur HashMap<NamaTabel, Isi tabel>
    HashMap<String,HashMap<String, ArrayList<String>>> database=new HashMap<>();
    public static void main(String[] args) throws Exception
    {
        //  HIDUPIN SERVER BUAT DENGERIN CLIENT
        
        //server inisialisasi
        server = new Server();

        //register packet class
        server.getKryo().register(PacketMessage.class);
        
        //binding port ke port client
        server.bind(tcpPort, udpPort);
        
        //start server
        server.start();
        
        //tambahin listener
        server.addListener(new serverSide());
        
        System.out.println("Server ngedengerin client");
        
        //  HIDUPIN SERVER BUAT DENGERIN SESAMA SERVER
        
        Server server1;
        //server inisialisasi
        server1 = new Server();

        //register packet class
        server1.getKryo().register(PacketMessage.class);
        
        server1.getKryo().register(HashMap.class);
        
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

        //client start
        client.start();

        ArrayList<String> ip=new ArrayList<>(); 
        ip.add("192.168.0.102");
        ip.add("127.0.0.1");
        
        
        //Mencoba melakukan koneksi dengan server jika gagal atau server masih belum nyala
        //akan menampilkan pesan
        for (int i = 0; i < ip.size(); i++)
        {

                try 
                {
                    client.connect(5000, ip.get(i), tcpServer, udpServer);
                    // minta data
                    if (!ip.get(i).equals(client.getRemoteAddressTCP().getAddress().getHostAddress()))
                    {
                           //Buat sebuah paket message
                           PacketMessage packetMessage = new PacketMessage();

                           //Buat sebuah pesannya
                           packetMessage.message = "get";
                           //c.sendTCP(packetMessage);
                           client.sendTCP(packetMessage);
                    }
                    
                }
                catch (Exception e) 
                {
                    System.out.println("Server "+ip.get(i)+" belum siap");
                }

        }
        client.addListener(new serverSide());
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
        if(p instanceof PacketMessage)
		{
			PacketMessage packet = (PacketMessage) p;
			System.out.println("Received : "+packet.message);
                        
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
                            createTable(parse[2], database, c);
                        }
                        else if(parse[0].toLowerCase().equals("insert") && parse.length==4 && cek)
                        {
                            insertTable(parse[1], parse[2], parse[3], c);
                        }
                        else if(parse[0].toLowerCase().equals("display") && parse.length==2 && cek)
                        {
                            displayTable(parse[1], c);
                        }
                        //digunakan untuk mengecek semua timestamp kesimpen atau ga
                        else if(parse[0].toLowerCase().equals("display") && parse[1].toLowerCase().equals("all") && parse.length==3 && cek)
                        {
                            displayAllTable(parse[2], c);
                        }
                        //kalau semua command tidak tepat
                        else
                        {
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
           client.sendTCP(packetMessage);
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
            
           Map<String, ArrayList<String>> tabelDummy = database.get(namaTabel);
           
           //text yang digunakan untuk menggabungkan string dari table
           String Mess = "\n";
           
           // untuk setiap kolom di masukin ke dalam string yang buat ngirim
           for (Map.Entry<String, ArrayList<String>> entry : tabelDummy.entrySet()) 
           {
            //menggunakan sizeLIst bertujuan agar dapat mengambil data dengan timestamp terbaru
            int sizeList=entry.getValue().size();
            Mess = Mess + entry.getKey() + " " + entry.getValue().get(sizeList-2) + " " + entry.getValue().get(sizeList-1) + "\n" ;
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
    public void createTable(String namaTabel,HashMap<String,HashMap<String, ArrayList<String>>> database,Connection client)
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
            HashMap<String, ArrayList<String>> dummy=new HashMap<>();
            database.put(namaTabel,dummy);
            
             //Buat sebuah paket message
            PacketMessage packetMessage = new PacketMessage();

            //Buat sebuah pesannya
            packetMessage.message = "Table "+namaTabel+" sudah di buat";

            //Kirim pesannya
            client.sendTCP(packetMessage);

        }
    }
    
    
}
           
