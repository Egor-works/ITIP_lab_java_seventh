import java.net.*;
// Набор пакетов для обеспечения безопасных Интернет-коммуникаций.
// В данной работе мы используем его преимущественно для работы с сокетами
// Сокеты - это средства для установления канала связи между машинами по сети.
import java.io.*;
// Обеспечивает системный ввод и вывод через потоки данных, сериализацию и файловую систему.
// Если не указано иное, передаёт нулевой аргумент конструктору или методу в любом классе
// или интерфейсе в этом пакете -> приводит к NullPointerException выбросу
import java.util.*;
// В утилитах нам понадобятся списки. Списки похожи на массивы объектов, за исключением того, что они
// могут легко менять размерность при необходимости, и в них не используются
// скобки для поиска отдельных элементов. В этой работе нам больше подойдёт реализация LinkedList списка, так как
// мы фактически оперируем цепочкой ссылок


/** Сканер (Crawler) - это класс, который
 перемещается по веб-страницам и ищет URL-адреса, поэтому класс сканера
 должен включать в себя код, который фактически открывает и закрывает
 сокеты. **/
public class Crawler {
    private static final LinkedList <URLDepthPair> myFindLinkedList = new LinkedList <>();
    private static final LinkedList <URLDepthPair> myViewedLinkedList = new LinkedList <>();

    /** Вывод результата **/
    public static void showResult(LinkedList<URLDepthPair> resultLink) {
        System.out.println();
        for(URLDepthPair c : resultLink) {
            String adjust = "";
            for(int i = 0; i < c.getDepth(); i++) {
                adjust = adjust.concat("  ");
            }
            System.out.println(adjust+c.getDepth() + "\tLink : "+c.getURL());
        }
    }


    /** Формулирование запроса **/
    public static void request(PrintWriter out,URLDepthPair pair) throws MalformedURLException {
        String request = "GET " + pair.getPath() + " HTTP/1.1\r\nHost:" + pair.getHost() + "\r\nConnection: Close\r\n";
        //Hyper Text Transfer Protocol (Протокол передачи
        //гипертекста). Это стандартный текстовый протокол, используемый для
        //передачи данных веб-страницы через Интернет. Последней спецификацией
        //HTTP является версия 1.1, которую будет использована в данной лабораторной
        //работе.
        out.println(request);
        out.flush();
    }

    /** Метод выполняющий основную работу**/
    public static void Scan(String temp, int maxDepth) throws IOException {
        myFindLinkedList.add(new URLDepthPair(temp, 0));
        while(!myFindLinkedList.isEmpty()) {
            URLDepthPair currentPair = myFindLinkedList.removeFirst();
            if(currentPair.getDepth() < maxDepth) {
                Socket mySocket;
                //Сокет(разъем) - это ресурс, предоставляемый операционной
                //системой, который позволяет вам обмениваться данными с другими
                //компьютерами по сети. Вы можете использовать сокет для установки
                //соединения с веб-сервером, но вы должны использовать TCP-сокет и
                //использовать протокол HTTP для того, чтобы сервер мог ответить.
                try {
                    mySocket = new Socket(currentPair.getHost(), 80);
                    //Порт: несколько разных программ на одном сервере могут слушать
                    //соединения через разные порты. Каждый порт обозначается номером в
                    //диапазоне 1..65535. Номера от 1 до 1024 зарезервированы для операционной
                    //системы. У большинства серверов есть порт по умолчанию. Для HTTP соединений обычно используется порт 80.
                } catch (UnknownHostException e) {
                    System.out.println("Could not resolve URL: "+currentPair.getURL()+" at depth "+currentPair.getDepth());
                    continue;
                }
                mySocket.setSoTimeout(10000);
                //void setSoTimeout(int timeout) устанавливает время ожидания сокета
                //(Socket) в миллисекундах. Данный метод необходимо вызвать после создания
                //сокета, чтобы он знал, сколько нужно ждать передачи данных с другой
                //стороны.
                try {
                    System.out.println("Now scanning: "+currentPair.getURL()+" at depth "+currentPair.getDepth());
                    BufferedReader in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
                    //Этот метод позволяет сокету получать данные с другой стороны
                    //соединения.
                    PrintWriter out = new PrintWriter(mySocket.getOutputStream(), true);
                    //Этот метод позволяет сокету отправлять данные на другую сторону
                    //соединения
                    request(out, currentPair);
                    String line= in.readLine();
                    System.out.println(line);
                    while ((line = in.readLine()) != null) {
                        //<a href="[любой_URL-адрес_начинающийся_с_http://]">
                        if (line.contains(currentPair.getURLPrefix())) {
                            StringBuilder currentLink = new StringBuilder();
                            int i = line.indexOf(currentPair.getURLPrefix());
                            while (line.charAt(i) != '"' && line.charAt(i) != ' ') {
                                if (line.charAt(i) == '<') {
                                    currentLink.deleteCharAt(currentLink.length() - 1);
                                    break;
                                }
                                else {
                                    currentLink.append(line.charAt(i));
                                    i++;
                                }
                            }
                            System.out.println(" > Found new link: "+ currentLink);
                            URLDepthPair newPair = new URLDepthPair(currentLink.toString(), currentPair.getDepth() + 1);
                            if (URLDepthPair.check(myFindLinkedList, newPair) && URLDepthPair.check(myViewedLinkedList, newPair) ){
                                myFindLinkedList.add(newPair);
                            }
                        }
                    }
                    mySocket.close();
                    //закрывает сокет
                } catch (SocketTimeoutException e) {
                    mySocket.close();
                }
                myViewedLinkedList.add(currentPair);
            }

        }
        showResult(myViewedLinkedList);
    }


    public static void main(String[] args) {
        //http://crawler-test.com/
        try {
            Scan(args[0], Integer.parseInt(args[1]));
        } catch (Exception e) {
            System.out.println("usage: java Crawler <URL><depth>" + e);
        }
    }
}