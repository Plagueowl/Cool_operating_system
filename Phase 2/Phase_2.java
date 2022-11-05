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

    class PCB{
        private int TTL;
        private int TLL;
        private int jobId;

        public void setJobId(int jobId) {
            this.jobId = jobId;
        }

        public void setTLL(int TLL) {
            this.TLL = TLL;
        }

        public void setTTL(int TTL) {
            this.TTL = TTL;
        }

        public int getJobId() {
            return jobId;
        }

        public int getTLL() {
            return TLL;
        }

        public int getTTL() {
            return TTL;
        }
    }
    File input;
    FileReader fr;


    public OS2() throws FileNotFoundException {
        //size allocation
        buffer = new char[40];
        memory = new char[300][4];

        IR = new char[4];
        R = new char [4];
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
        TTC = 0;
        LLC = 0;
        EM = 0;
        TI = 0;
        PI = 0;
        used_frames = new int[30];




    }


    public void MOS(){
        if(PI != 0)
            switch (PI){
            case 1:
                if(TI == 0){
                    terminate(4);
                }
                else{
                    terminate(3);
                    terminate(4);
                }
                break;
            case 2:
                if(TI == 0){
                    terminate(5);
                }
                else{
                    terminate(3);
                    terminate(5);
                }
                break;
            case 3:
                if(TI == 0){
                    System.out.println("page fault");
                    if(SI == 1 || (IR[0] == 'S' && IR[1] == 'R')){
                        System.out.println("Resolving..");
                        int temp3 = Allocate()*10;
                        int i = PTR;
                        while(memory[i][0]!='@'){i++;}
                        memory[i][0] = '0';
                        memory[i][3] = (char)(temp3%10 + '0');
                        temp3/=10;
                        memory[i][2] = (char)(temp3%10 + '0');
                        temp3/=10;
                        memory[i][1] = (char)(temp3%10 + '0');
                    }
                    else{
                        terminate(6);
                    }
                }
                if(TI == 2){
                    terminate(3);
                }



        }
        else
            switch (SI){
            case 1:
                if(TI == 0){
                    try {
                        read();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    terminate(3);
                }
                break;

            case 2:
                if(TI == 0){
                    write();
                }
                else{
                    write();
                    terminate(3);
                }
                break;
            case 3:
                terminate(0);
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
            for (buffer_ptr = 0; buffer_ptr < temp.length && buffer_ptr<40; buffer_ptr++) {
                buffer[buffer_ptr] = temp[buffer_ptr];
            }
            System.out.println(buffer);
        }
        if (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {  //Add D of $END later
            System.out.println("Program Over");
            terminate(1);
            return;
        }
        int ra = realAddress((IR[2]-'0') *10);
        if(PI == 3){
            MOS();
            PI = 0;
        }
        ra = realAddress((IR[2]-'0') *10);

        load_memory_data(ra);
        buffer_reset();

    }


    public void write(){
        System.out.println("Write executed");
        IR[3] = '0';
        int buffer_ptr;
        char [] temp;
        try{
            int memory_ptr = realAddress((IR[2]-'0')*10);
            if(memory_ptr == -1){
                PI = 2;
                MOS();
            }
            else{
                int buffer_ptr4 = 0;
                int limit = memory_ptr+10;
//            FileWriter myWriter = new FileWriter("src/com/company/out.txt");

                for(memory_ptr = realAddress((IR[2]-'0')*10);memory_ptr<limit;memory_ptr++){
                    for(int i = 0;i<4;i++){
                        if(memory[memory_ptr][i] == '@')
                            continue;
                        buffer[buffer_ptr4] = memory[memory_ptr][i];
                        buffer_ptr4++;
                    }
                }
                StringBuffer sb = new StringBuffer();
                buffer_ptr4 = 0;
//                System.out.println(buffer);
                while(buffer_ptr4 < 40 && buffer[buffer_ptr4]!='@'){
                    sb.append(buffer[buffer_ptr4]);
                    buffer_ptr4++;
                }
                sb.append('\n');
                System.out.println("sb is"+sb);
                Files.write(Paths.get("src/com/company/out.txt"), String.valueOf(sb).getBytes(), StandardOpenOption.APPEND);

//            myWriter.append(String.valueOf(sb));
//            myWriter.flush();
//            myWriter.close();
                buffer_reset();

            }
        }
        catch(IOException e){
            System.out.println("Exception occured at write()");
            e.printStackTrace();
        }


    }


    public void terminate(int EM){
        try{
            switch (EM){
                case 0:
                    Files.write(Paths.get("src/com/company/out.txt"), "\n\n".getBytes(), StandardOpenOption.APPEND);
                    break;
                case 1:
                    Files.write(Paths.get("src/com/company/out.txt"), "\n\nOut of Data".getBytes(), StandardOpenOption.APPEND);
                    break;
                case 2:
                    Files.write(Paths.get("src/com/company/out.txt"), "\n\nLine limit exceeded".getBytes(), StandardOpenOption.APPEND);
                    break;
                case 3:
                    Files.write(Paths.get("src/com/company/out.txt"), "\n\nTime Limit Exceeded".getBytes(), StandardOpenOption.APPEND);
                case 4:
                    Files.write(Paths.get("src/com/company/out.txt"), "\n\nOP code error".getBytes(), StandardOpenOption.APPEND);
                case 5:
                    Files.write(Paths.get("src/com/company/out.txt"), "\n\nOperand error".getBytes(), StandardOpenOption.APPEND);
                case 6:
                    Files.write(Paths.get("src/com/company/out.txt"), "\n\nInvalid page fault".getBytes(), StandardOpenOption.APPEND);

            }
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
        int ra = realAddress(IC);
        while (IC<90 && memory[ra][0] != '@') {
            int ra2;
            for (int i = 0; i < 4; i++) {
                IR[i] = memory[ra][i];
            }
            IC++;
            ra = realAddress(IC);

            switch (IR[0]) {
                case 'L':
                    if(IR[1] == 'R'){
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3)
                            MOS();
                        else{
                            for(int i = 0;i<4;i++){
                                R[i] = memory[ra2][i];
                            }
                        }
                    }
                    TTC++;
                    break;
                case 'S':
                    if(IR[1] == 'R'){
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3)
                            MOS();
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));

                        for(int i = 0;i<4;i++){
                            memory[ra2][i] = R[i];
                        }
                    }
                    TTC+=2;
                    break;
                case 'C':
                    if (IR[1] == 'R') {
                        ra2 = (IR[2] - '0')*10 + (IR[3] - '0');
                        if(PI == 3)
                            MOS();
                        else{
                            comparing(ra2);

                        }
                    }
                    TTC++;
                    break;

                case 'B':
                    if(IR[1] == 'T'){
                        ra2 = (IR[2] - '0')*10 + (IR[3] - '0');
                        if(PI == 3)
                            MOS();
                        else{
                            if(toggle){
                                IC = (IR[2] - '0') *10 + (IR[3] - '0');
                            }
                        }

                    }
                    TTC++;
                    break;

                case 'G':
                    if (IR[1] == 'D') {
                        SI = 1;
                        MOS();
                        SI = 0;
                    }
                    TTC+=2;
                    break;
                case 'P':
                    if (IR[1] == 'D') {
                        SI = 2;
                        MOS();
                        SI = 0;
                    }
                    TTC++;
                    break;
                case 'H':
                    SI = 3;
                    MOS();
                    SI = 0;
                    TTC++;
                    break;
                default:
                    PI = 1;
                    MOS();






            }

        }
    }



    public void comparing(int c) {




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
                    PCB pcb = new PCB();
                    pcb.setJobId(((buffer[4] - '0')*1000) + ((buffer[5] - '0')*100) + ((buffer[6] - '0')*10) + ((buffer[7] - '0')));
                    pcb.setTTL(((buffer[8] - '0')*1000) + ((buffer[9] - '0')*100) + ((buffer[10] - '0')*10) + ((buffer[11] - '0')));
                    pcb.setTLL(((buffer[12] - '0')*1000) + ((buffer[13] - '0')*100) + ((buffer[14] - '0')*10) + ((buffer[15] - '0')));
                    //page allocation
                    System.out.println("Job id: " + pcb.getJobId());
                    System.out.println("TTL: " + pcb.getTTL());
                    System.out.println("TLL: " + pcb.getTLL());

                    PTR = Allocate()*10;


                    buffer_reset();
                    continue;
                }
                else if (buffer[0] == '$' && buffer[1] == 'D' && buffer[2] == 'T' && buffer[3] == 'A') {
                    //do something
//                    return;
                    buffer_reset();
//                    mem_ptr = (mem_ptr % 10 == 0) ? mem_ptr : ((mem_ptr / 10 + 1) * 10);
                    MOSstartExec();
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
        if(mem_ptr>=300){
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
        if(memory[pte][0] == '@') {
            PI = 3;
            return -1;
        }

        int ra = (memory[pte][1]-'0') * 100 + (memory[pte][2] - '0')*10 + (memory[pte][3] - '0') + VA%10; //real address
        return ra;
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


