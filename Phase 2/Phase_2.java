package com.company;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;



public class Phase_2 {
    public static void main(String[] args) {
        OS2 obj = null;
        try {
            obj = new OS2();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        obj.load();
        obj.printer();
    }

}

class OS2{
    char [] buffer;
    char [][] memory;
    int mem_ptr;
    //CPU
    char [] IR;
    char [] R;
    int PTR;
    int IC;
    boolean toggle;
    //counters
    int TTL;
    int TLL;
    int TTC;
    int LLC;
    //interrupts
    int SI; //system Interrupt
    int TI; //Time interrupt
    int PI; //page interrupt
    int EM; //errror message;

    //miscellaneous
    int [] used_frames; //keeps track of the frames used
    int temp1;   //temporary usage variable
    Random random;

    File input;
    FileReader fr;

    public OS2() throws FileNotFoundException {
        //size allocation
        buffer = new char[40];
        memory = new char[300][4];

        IR = new char[4];
        R = new char [4];
        used_frames = new int[30];
        random = new Random();

        value_allocator();

    }


    private void value_allocator() throws FileNotFoundException {
        //value allocation
        for(int i = 0;i<4;i++){
            IR[i] = '@';
            R[i] = '@';
        }
        buffer_reset();
        for(int i = 0;i < 300; i++){

            for(int j = 0; j< 4;j++){
                memory[i][j] = '@';
            }
        }
        IC = 0;
        toggle = false;
        mem_ptr = 0;
        SI = 0;
        TTL = 0;
        TTC = 0;
        TLL = 0;
        LLC = 0;
        EM = 0;
        TI = 0;
        PI = 0;




    }


    public void MOS(){
        switch (SI){
            case 1:
                try {
                    read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case 2:
                write();
                break;
            case 3:
                halt();
                break;

        }
    }


    public void read() throws IOException {
        System.out.println("Read executed");
        IR[3] = '0';
        int buffer_ptr;
        char [] temp;
        if(reader.ready()){
            temp = reader.readLine().toCharArray();
            for (buffer_ptr = 0; buffer_ptr < temp.length; buffer_ptr++) {
                buffer[buffer_ptr] = temp[buffer_ptr];
            }
            System.out.println(buffer);
        }
        if (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {  //Add D of $END later
            System.out.println("Program Over");
            return;
        }

        load_memory_data((IR[2]-'0')*10);
        buffer_reset();

    }


    public void write(){
        System.out.println("Write executed");
        IR[3] = '0';
        int buffer_ptr;
        char [] temp;
        try{
            IR[3] = '0';
            int memory_ptr = (IR[2]-'0')*10;
            int buffer_ptr4 = 0;
            int limit = memory_ptr+10;
//            FileWriter myWriter = new FileWriter("src/com/company/out.txt");

            for(memory_ptr = (IR[2]-'0')*10;memory_ptr<limit && memory[memory_ptr][0] != '@' ;memory_ptr++){
                for(int i = 0;i<4;i++){
                    if(memory[memory_ptr][i] == '@')
                        break;
                    buffer[buffer_ptr4] = memory[memory_ptr][i];
                    buffer_ptr4++;
                }
            }
            StringBuffer sb = new StringBuffer();
            buffer_ptr4 = 0;
            while(buffer_ptr4 < 40 && buffer[buffer_ptr4]!='@'){
                sb.append(buffer[buffer_ptr4]);
                buffer_ptr4++;
            }
            sb.append('\n');
            Files.write(Paths.get("src/com/company/out.txt"), String.valueOf(sb).getBytes(), StandardOpenOption.APPEND);

//            myWriter.append(String.valueOf(sb));
//            myWriter.flush();
//            myWriter.close();
            buffer_reset();
        }
        catch(IOException e){
            System.out.println("Exception occured at write()");
            e.printStackTrace();
        }


    }


    public void halt(){
        try{
            System.out.println("Halt called");
            Files.write(Paths.get("src/com/company/out.txt"), "\n\n".getBytes(), StandardOpenOption.APPEND);
            IC = 100;
            return;
        }
        catch(IOException e){
            System.out.println("Exception occured at halt()");
            e.printStackTrace();
        }
    }


    public void MOSstartExec(){

        System.out.println("Exec started");
        IC = 0;
        executeUserProgram();   //slave mode
    }



    public void executeUserProgram(){
        //loading IR
        while (IC<90 && memory[IC][0] != '@') {
            for (int i = 0; i < 4; i++) {
                IR[i] = memory[IC][i];
            }
            IC++;
            switch (IR[0]) {
                case 'L':
                    if(IR[1] == 'R'){
                        for(int i = 0;i<4;i++){
                            R[i] = memory[(IR[2]-'0')*10 + (IR[3] -'0')][i];
                        }
                    }
                    break;
                case 'S':
                    if(IR[1] == 'R'){
                        for(int i = 0;i<4;i++){
                            memory[(IR[2]-'0')*10 + (IR[3] -'0')][i] = R[i];
                        }
                    }
                    break;
                case 'C':
                    if (IR[1] == 'R') {
                        char a =IR[2];
                        char b =IR[3];
                        comparing(a,b);
                    }
                    break;

                case 'B':
                    if(IR[1] == 'T'){
                        if(toggle == true){
                            IC = (IR[2] - '0') *10 + (IR[3] - '0');
                        }
                    }
                    break;

                case 'G':
                    if (IR[1] == 'D') {
                        SI = 1;
                        MOS();
                    }
                    break;
                case 'P':
                    if (IR[1] == 'D') {
                        SI = 2;
                        MOS();
                    }
                    break;
                case 'H':
                    SI = 3;
                    MOS();
                    break;
                default:
                    PI = 1;






            }
        }
    }



    public void comparing( char a,char b) {


        int c = (a-'0') * 10 + (b-'0') ;

        for (int i = 0; i < 4; i++) {
//            if (R[i] == '@') {
//                toggle = false;
//                return;
//            }
            if (R[i] == memory[c][i]) {
                continue;
            } else {
                toggle = false;
                return;
            }
        }
        toggle = true;
        return ;
    }


    private static java.io.File file;
    private static BufferedReader reader;
    static {
        try {
            file = new java.io.File("src/com/company/test.txt");
            reader = new BufferedReader(new FileReader(String.valueOf(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void load(){

        //loading from card to buffer

        try {
            do {
                int buffer_ptr = 0;
                char [] temp;
                temp = reader.readLine().toCharArray();
                for (buffer_ptr = 0; buffer_ptr < temp.length; buffer_ptr++) {
                    buffer[buffer_ptr] = temp[buffer_ptr];
                }
                System.out.println(buffer);

                if (buffer[0] == '$' && buffer[1] == 'A' && buffer[2] == 'M' && buffer[3] == 'J') {
                    TTL = ((buffer[8] - '0')*1000) + ((buffer[9] - '0')*100) + ((buffer[10] - '0')*10) + ((buffer[11] - '0')); // getting TTL
                    TLL = ((buffer[12] - '0')*1000) + ((buffer[13] - '0')*100) + ((buffer[14] - '0')*10) + ((buffer[15] - '0')); // Getting TLL
                    //page allocation
                    PTR = Allocate()*10;


                    buffer_reset();
                    continue;
                }
                else if (buffer[0] == '$' && buffer[1] == 'D' && buffer[2] == 'T' && buffer[3] == 'A') {
                    //do something
//                    System.out.println(buffer);
                    return;
//                    buffer_reset();
//                    mem_ptr = (mem_ptr % 10 == 0) ? mem_ptr : ((mem_ptr / 10 + 1) * 10);
//                    MOSstartExec();
                }
                else if (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {  //Add D of $END later
                    System.out.println("Program Over");
                    printer();
                    value_allocator();
                }
                else{

                    //instruction frame allocation and loading
                    temp1 = Allocate()*10;
                    System.out.println(temp);

                    //Page table update;
                    int i = PTR;
                    while(memory[i][0]!='@'){i++;}
                    load_memory_instructions(temp1);

                    memory[i][0] = '0';
                    memory[i][3] = (char)(temp1%10 + '0');
                    temp1/=10;
                    memory[i][2] = (char)(temp1%10 + '0');
                    temp1/=10;
                    memory[i][1] = (char)(temp1%10 + '0');


                    buffer_reset();

                }
                if (buffer_ptr > 40) {
                    System.out.println("Buffer Overload, quitting...");
                    return;
                }
            }while(reader.ready());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void load_memory_instructions(int address){
        int buffer_ptr2 = 0;
        int limit = address+10;
        while(buffer_ptr2<40 && buffer[buffer_ptr2]!='@' && address<limit){
            for(int i = 0;i<4;i++){
                if(buffer[buffer_ptr2] == '@')
                    break;
                memory[address][i] = buffer[buffer_ptr2];
                if(buffer[buffer_ptr2]=='H'){
                    buffer_ptr2++;
                    break;
                }
                buffer_ptr2++;
            }
            address++;
        }
//        printer();
    }


    public void load_memory_data(int location){
        mem_ptr = location;
        if(mem_ptr>=100){
            System.out.println("Memory Overload, quitting...");
            return;
        }
        int buffer_ptr1 = 0;
        while(buffer_ptr1<40 && buffer[buffer_ptr1]!='@'){
            for(int i = 0;i<4;i++){
                if(buffer[buffer_ptr1] == '@')
                    break;
                memory[mem_ptr][i] = buffer[buffer_ptr1];
                buffer_ptr1++;
            }
            mem_ptr++;
        }

    }


    public void buffer_reset(){
        for(int i = 0;i< 40;i++){
            buffer[i] = '@';
        }
    }

    private int Allocate(){
        int temp2;
        while(used_frames[temp2 = random.nextInt(30)] != 0){}
        used_frames[temp2] = 1;
        return temp2;

    }

    private int realAddress(int VA){
        int pte = PTR + VA/10;
        return ((memory[pte][2] - '0')*10 + (memory[pte][3] - '0')) *10 + VA%10; //return real address
    }





    public void printer(){
        System.out.println("Buffer is: ");
        System.out.println(buffer);
        System.out.println("IC is:");
        System.out.println("Used frames are: ");
        for(int i = 0;i<30;i++){
            System.out.print(used_frames[i] );
        }
        System.out.println(IC);
        System.out.println("Main memory");
        for(int i = 0; i< 300;i++){
            System.out.print(i+" : ");
            for(int j = 0;j<4;j++){
                System.out.print(memory[i][j]);
            }
            System.out.println();
        }
        System.out.println("PTR: "+PTR );
        System.out.println("IR:");
        System.out.println(IR);
        System.out.println("R:");
        System.out.println(R);
        System.out.println("Toggle:");
        System.out.println(toggle);

    }

}
