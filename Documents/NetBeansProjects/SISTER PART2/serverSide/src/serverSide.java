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
    //Port yang digunakan untuk nyambung ke server
    static int udpServer=44444,tcpServer=44444;
    
    //variabel digunakan untuk kirim atau proses ke server yang lain
    static String namaTabel="";
    static String key="";
    static String value="";
    
    //range Token yang dimiliki oleh server dengan maksimal 2^31
    static int startToken=0;
    static int endToken=0;
    
    //digunakan untuk menentukan apakah semua data sudah terkumpul semua
    static int counter = 0;
    
    //variabel yang digunakan untuk menentukan perintah yang akan di eksekusi
    static boolean create=false;
    static boolean insert=false;
    static boolean display=false;
    
    //Connection yang digunakan untuk mengirim ke client yang terhubung ke server
    static Connection requestConn;
    
    //jumlah hashCode yang dapat tertampung di dalam server
    static int maxSize = 0;
    
    //IP server yang mempunyai jumlah data yang paling besar
    static String IPmaxSize = "";
    
    //data yang akan di cetak
    static String Mess;

    //list IP server
    static ArrayList<String> ip=new ArrayList<>();
    
    //representasi database dengan struktur HashMap<NamaTabel, Isi tabel>
    static HashMap<String,HashMap<String, ArrayList<String>>> database=new HashMap<>();
    
    
    public static void main(String[] args) throws Exception
    {
        //  HIDUPIN SERVER BUAT DENGERIN CLIENT
        
        //server inisialisasi
        server = new Server();

        //binding port ke port client
        server.bind(tcpPort, udpPort);
        
        //start server
        server.start();
        
        //tambahin listener
        server.addListener(new serverSide());
                
        System.out.println("Server ngedengerin client");
        
        //  HIDUPIN SERVER BUAT DENGERIN SESAMA SERVER
   
        //server yang digunakan untuk mendengarkan dari sesama server
        server1 = new Server();

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

         
        //Daftar IP server
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
                    //pesan jika server belum nyala atau eror
                    System.out.println("Server "+ip.get(i)+" belum siap");
                }
            
        }
        
        //menunggu sampai semua server menjawab
        while(counter!=0)
        {
            Thread.sleep(100);
        }
        
        //jika tidak ada server yang hidup
        if (maxSize == 0)
        {
            startToken=0;
            endToken=(int) Math.pow(2, 31);
        }
        
        //jika ada server yang hidup minta data dari server yang mempunyai maxData yang terbesar
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
       
       //menunggu perintah yang diberikan dari client
       while(true)
       {
           //jika perintah dalam bentuk create
           if(create)
           {
               //kirim ke semua server yang masih hidup
               for(int i=0;i<allClient.size();i++)
               {
                     //Buat sebuah paket message
                    PacketMessage packetMessage = new PacketMessage();

                    //Buat sebuah pesannya
                    packetMessage.message = "create server "+namaTabel;
                    
                    
                    //di cek apakah masih tersambung atau tidak. 
                    if(allClient.get(i).isConnected())
                    {    
                    //Kirim pesannya
                    allClient.get(i).sendTCP(packetMessage);
                    }
               }
               create=false;
           }
           
           //jika perintah dari server berisi insert
           if(insert)
           {
               for(int i=0;i<allClient.size();i++)
               {
                     //Buat sebuah paket message
                    PacketMessage packetMessage = new PacketMessage();

                    //Membuat pesan yang berisi perintah untuk meminta data ke server lain
                    packetMessage.message = "insert server "+namaTabel+" "+key+" "+value;
                    
                    //cek apakah masih connect atau tidak
                    if(allClient.get(i).isConnected())
                    {    
                    //Kirim pesannya
                    allClient.get(i).sendTCP(packetMessage);
                    }
               }

               insert=false;
           }
           
           //jika perintah yang diberikan oleh user adalah display
           if(display)
           {
               for(int i=0;i<allClient.size();i++)
               {
                     //Buat sebuah paket message
                    PacketMessage packetMessage = new PacketMessage();

                    //Buat sebuah pesan yang digunakan untuk meminta ke server lain
                    packetMessage.message = "display server "+namaTabel;

                    //cek apakah connection masih tersambung atau tidak
                    if(allClient.get(i).isConnected())
                    {    
                        allClient.get(i).getKryo().register(Tabel.class);
                        //Kirim pesannya
                        allClient.get(i).sendTCP(packetMessage);
                        
                        //tambah counter agar dapat menunggu semua server membalas atau memberikan data yang dimiliki
                        counter++;
                        
                    }
               }
               
               //menunggu semua data terkumpul di server yang nyambung ke server
               while (counter > 0)
               {
                   Thread.sleep(100);
               }

               display=false;
               
               //membuat isi dari tabel yang di cari oleh client
               PacketMessage packetMessage = new PacketMessage();
               packetMessage.message = Mess;
               
               //mengirim data yang sudah di kumpulkan ke client yang meminta
               requestConn.sendTCP(packetMessage);
           }
           Thread.sleep(100);
       } 
        
    }
    
    //Ini dijalankan kalo dapet koneksi
    public void connected(Connection c)
    {
        System.out.println("Received a connection from "+c.getRemoteAddressTCP().getHostName());
    }
    
    //Ini dijalankan saat kita menerima paket
    public void received (Connection c, Object p)
    {
        //memproses data jika data yang di terima turunan kelas HashMap
        if(p instanceof HashMap)
        {
            //masukkan data yang diterima ke database lokal
            database=(HashMap<String, HashMap<String, ArrayList<String>>>) p;
            
            System.out.println("database sudah diterima dari"+c.getRemoteAddressTCP().getAddress().getHostAddress());
            
            //variabel yang digunakan untuk menyimpan data dari tabel yang tidak sesuai dengan token server
            ArrayList<String> listTabel=new ArrayList<>();
            ArrayList<String> isiTabel=new ArrayList<>();
            
            //melakukan cek apakah untuk setiap data masuk ke dalam token yang dimiliki oleh erver
            //jika tidak sesuai maka masuk ke dalam list hapus
            for (Map.Entry<String,HashMap<String, ArrayList<String>>> entry : database.entrySet()) 
            {
                for (Map.Entry<String, ArrayList<String>> entry1 : entry.getValue().entrySet()) 
                {
                    //ubah key dari data menjadi HashCode
                    int code = Math.abs(entry1.getKey().hashCode());
                    
                    //cek apakah sesuai dalam range token yang dimiliki server atau tidak
                    if (!(code >= startToken && code < endToken))
                    {
                        //tambah ke dalam list hapus
                        listTabel.add(entry.getKey());
                        isiTabel.add(entry1.getKey());
                    }

                }
            }
            
            //menghapus data yang tidak sesuai dengan token sesuai dengan daftar list data yang akan
            //di hapus
            for(int i=0;i<listTabel.size();i++)
            {
                database.get(listTabel.get(i)).remove(isiTabel.get(i));
            }

        }
        
        //proses jika pesan yang diterima dalam bentuk PacketMessage
        if(p instanceof PacketMessage)
		{
                    PacketMessage packet = (PacketMessage) p;
                    
                    //jika paket tidak kosong
                    if(packet.message != null) 
                    {
                        //menampilkan pesan
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
                        
                        /*proses cek input an bakal di proses kayak gimana*/
                        
                        //jika yang diterima create table bla bla bla;
                        if(parse[0].toLowerCase().equals("create") && parse[1].toLowerCase().equals("table") && parse.length==3 && cek)
                        {
                            
                            //ubah nama tabel berdasarkan perintah yang diterima
                            namaTabel=parse[2];
                            
                            //cek apakah tabel yang akan di create sudah ada atau tidak
                            if (database.get(namaTabel)!= null)
                            {
                                   //Buat sebuah paket message
                                   PacketMessage packetMessage = new PacketMessage();

                                   //Buat sebuah pesannya
                                   packetMessage.message = "Table "+namaTabel+" sudah ada";

                                   //Kirim pesannya
                                   c.sendTCP(packetMessage);
                            }
                            
                            //jika tidak ada maka perintah di proses
                            else
                            {
                                create = true;
                                PacketMessage packetMessage = new PacketMessage();
                                
                                //kirim pesan ke client yang nyambung
                                packetMessage.message = "Tabel berhasil dibuat";
                                c.sendTCP(packetMessage);
                            }
                            
                        }
                        
                        //proses jika create yang dikirim dari server pusat atau server yang langsung terhubung
                        //ke server
                        else if (parse[0].toLowerCase().equals("create") && parse[1].toLowerCase().equals("server") && parse.length==3)
                        {
                            
                            //masukkan ke dalam database
                            HashMap<String, ArrayList<String>> dummy=new HashMap<>();
                            database.put(parse[2],dummy);
                        }
                        
                        //proses jika insert yang meminta dari server yang langsung nyambung ke client
                        else if(parse[0].toLowerCase().equals("insert") && parse[1].toLowerCase().equals("server") && parse.length==5)
                        {
                            //cek apakah sesuai dengan token yang di miliki oleh server
                            if(Math.abs(parse[3].hashCode()) >= startToken && Math.abs(parse[3].hashCode()) < endToken) 
                            {
                                //masukkan ke dalam tabel
                                insertTable(parse[2], parse[3], parse[4], c);
                            }
                        }
                        
                        //insert yang diberikan oleh client
                        else if(parse[0].toLowerCase().equals("insert") && parse.length==4 && cek)
                        {
                            //simpan connection yang dimiliki oleh client yang terhubung dengan server
                            requestConn = c;
                            namaTabel=parse[1];
                            
                            //cek apakah nama tabel tidak ada
                            if (database.get(namaTabel)== null)
                            {
                                   //Buat sebuah paket message
                                   PacketMessage packetMessage = new PacketMessage();

                                   //Buat sebuah pesannya
                                   packetMessage.message = "Table "+namaTabel+" tidak ada";

                                   //Kirim pesannya
                                   c.sendTCP(packetMessage);
                            }
                            
                            //jika tabel terdaftar atau ada dalam database
                            else
                            {
                                //set variabel agar dapat di olah
                                insert = true;
                                key = parse[2];
                                value = parse[3];
                            }
                        }
                        
                        //perintah display yang diminta dari client
                        else if(parse[0].toLowerCase().equals("display") && parse.length==2 && cek)
                        {
                            //reset data yang akan di lempar ke client
                            Mess = "\n";
                            display = true;
                        }
                        
                        //perintah yang diminta dari server untuk mengumpulkan semua data
                        else if(parse[0].toLowerCase().equals("display") && parse[1].toLowerCase().equals("server") && parse.length==3)
                        {
                            //konstruksi semua data yang dimiliki dalam tabel yang dicari
                            String temps = "";
                            Map<String, ArrayList<String>> tabelDummy = database.get(parse[2]);
                            for (Map.Entry<String, ArrayList<String>> entry : tabelDummy.entrySet()) 
                            {
                                //menggunakan sizeLIst bertujuan agar dapat mengambil data dengan timestamp terbaru
                                int sizeList=entry.getValue().size();
                                temps = temps + entry.getKey() + " " + entry.getValue().get(sizeList-2) + " " + entry.getValue().get(sizeList-1) + " \n " ;
                            }
                            
                            //kirim data ke server yang meminta dengan menambahi penanda perintah dengan "mess"
                            packet.message="mess "+temps;
                            c.sendTCP(packet);
                        }
                        
                        //jika data yang diterima mempunyai mess di huruf pertama nya
                        else if (parse[0].equals("mess"))
                        {
                            //gabungin data yang diterima dari beberapa server
                            for (int i = 1; i < parse.length; i++)
                            {
                                Mess = Mess + parse[i]+ " ";
                            }

                            //mengurangi counter agar biar tahu kalau semua data sudah diterima
                            //dengan cek apakah counter sudah menjadi 0 atau tidak
                            counter--;
                        }
                        
                        //digunakan untuk mengecek semua timestamp kesimpen atau ga
                        else if(parse[0].toLowerCase().equals("display") && parse[1].toLowerCase().equals("all") && parse.length==3 && cek)
                        {
                            displayAllTable(parse[2], c);
                        }
                        
                        //perintah ke server lain untuk mengetahui semua range token dan size data dari server tersebut
                        else if(parse[0].equals("size") && parse.length==1)
                        {
                            packet.message="size "+(endToken-startToken)+" "+endToken;
                            c.sendTCP(packet);
                        }
                        
                        //mengolah data jumlah token, dan range token yang diberikan dari server lain
                        else if(parse[0].equals("size") && parse.length==3)
                        {
                            //cek apakah maxsize sekarang lebih besar dari maxsize dari server yang memberikan datanya
                            if(maxSize<Integer.parseInt(parse[1]))
                            {
                                maxSize=Integer.parseInt(parse[1]);
                                IPmaxSize=c.getRemoteAddressTCP().getAddress().getHostAddress();
                                endToken=Integer.parseInt(parse[2]);
                                startToken=Integer.parseInt(parse[1])/2;
                            }
                            
                            //digunakan untuk mengetahui apakah semua data diterima atau tidak
                            //dengan cara cek counter sudah balik ke 0 atau tidak
                            counter--;
                            
                        }
                        
                        //perintah yang digunakan untuk meminta data ke server yang paling besar
                        //dan mengirim data ke server yang meminta
                        else if(parse[0].equals("get") && parse.length==1)
                        {
                           
                            endToken=(endToken-startToken)/2; 
                           
                            //variable yang digunakan untuk menentukan data mana saja yang
                            //tidak masuk dalam kategori server
                            ArrayList<String> listTabel=new ArrayList<>();
                            ArrayList<String> isiTabel=new ArrayList<>();
                            
                            //cek apakah untuk setiap data masuk dalam range token atau tidak
                            HashMap<String,HashMap<String, ArrayList<String>>> databaseKirim=(HashMap<String,HashMap<String, ArrayList<String>>>) database.clone();
                            for (Map.Entry<String,HashMap<String, ArrayList<String>>> entry : databaseKirim.entrySet()) 
                            {   
                                for (Map.Entry<String, ArrayList<String>> entry1 : entry.getValue().entrySet()) 
                                {
                                    //ubah jadi hashCode dulu
                                    int code = Math.abs(entry1.getKey().hashCode());
                                    
                                    //cek apakah masuk dalam kriteria atau tidak jika tidak dimasukkan kedalam daftar hapus
                                    if (!(code >= startToken && code < endToken))
                                    {
                                        listTabel.add(entry.getKey());
                                        isiTabel.add(entry1.getKey());
                                    }

                                }
                            }
                            
                            //mengirim data yang dimiliki dari server
                            c.sendTCP(databaseKirim);
                            
                            //menghapus data yang tidak sesuai dengan range token berdasarkan daftar hapus
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
                        } 
                        else {
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
