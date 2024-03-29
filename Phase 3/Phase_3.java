package com.company;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;



public class Phase_3 {
    public static void main(String[] args) {
        OS3 obj = null;
        try {
            obj = new OS3();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        obj.start();

    }

}

class OS3{
    char [] buffer;
    char [][] supervisoryStorage;
    char [] bufferStat;
    char [][] memory;       //RAM
    char [][] drum;     //secondary storage
    int mem_ptr;

    //CPU
    char [] IR;
    char [] R;
    int PTR;
    int IC;
    boolean toggle;
    //queues
    Queue<PCB> RQ,TQ,LQ,IoQ; //ready, terminate, load, I/O
    static Queue<Integer> ifb , ofb , ebq;  // inputful, outputful, empty buffers



    //interrupts
    int SI; //system Interrupt
    int TI; //Time interrupt
    int PI; //page interrupt
    int EM; //errror message;
    int IRi; //Interrupt service routine
    int IOI; // Input output interrupt


    //timers
    int time;
    int TSC;
    //channels
    boolean[] CH;
    int[] CHT;
    int[] CH_TOT;


    //miscellaneous
    int [] used_frames_ram; //keeps track of the frames used in ram
    int [] used_frames_drum; //keeps track of the frames used in drum
    Random random;
    PCB pcb; // pcb data structure;
    PCB job; //for channel 3
    char F; //flow status for channel 3;
    char[] task; // for channel 3;



    static class PCB{
        private int TTL;
        private int TLL;
        private int jobId;
        int LLC;
        int TTC;
        int Ptrack;
        int Pcards;
        int Dtrack;
        int Dcards;
        int Otrack;
        int Olines;



        //miscellaneous variables which store extra information about a job
        int loadedPcards;
        int loadedDcards;
        int loadedOcards;

        Integer PTR_ram;
        Integer PTR_drum;
        int terminate_stat;

        //CPU state
        int IC;
        char [] R_state; //holds general purpose register information
        boolean toggle_state;



        public PCB(){
            this.LLC = 0;
            this.TTC = 0;
            this.loadedDcards = 0;
            this.loadedPcards = 0;
            this.loadedOcards = 0;
            this.IC = 0;
            this.Otrack = -1;
            this.PTR_ram= null;
            this.R_state = new char[4];
            this.R_state[0] = '@';
            this.R_state[1] = '@';
            this.R_state[2] = '@';
            this.R_state[3] = '@';
            this.toggle_state = true;
        }


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
            return this.jobId;
        }

        public int getTLL() {
            return this.TLL;
        }

        public int getTTL() {
            return this.TTL;
        }
    }



    public OS3() throws FileNotFoundException {
        //size allocation
        buffer = new char[40];
        memory = new char[300][4];
        drum = new char[500][4];
        supervisoryStorage = new char[10][40];
        IR = new char[4];
        R = new char [4];
        random = new Random();
        RQ = new LinkedList<>();
        TQ = new LinkedList<>();
        LQ = new LinkedList<>();
        IoQ = new LinkedList<>();
        ifb = new LinkedList<>();
        ofb = new LinkedList<>();
        ebq = new LinkedList<>();
        bufferStat = new char[10];
        CH = new boolean[4];
        CH_TOT = new int[4];
        CHT = new int[4];
        task = new char[2];
        used_frames_ram = new int[30];
        used_frames_drum = new int[50];

        value_allocator();

    }
    private static java.io.File file;
    private static BufferedReader reader;
    static {
        try {
            file = new java.io.File("src/com/company/input3.txt");
            reader = new BufferedReader(new FileReader(String.valueOf(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void value_allocator() {
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
        for(int i = 0;i < 500; i++){

            for(int j = 0; j< 4;j++){
                drum[i][j] = '@';
            }
        }
        IC = 0;
        toggle = false;
        mem_ptr = 0;
        SI = 0;
        EM = 0;
        TI = 0;
        PI = 0;
        IRi = 0;
        time = 0;
        TSC = 9;
        for(int i = 0;i<10;i++){
            ebq.add(i);
            bufferStat[i] = 'E';
        }

        CH_TOT[1] = 5;
        CH_TOT[2] = 5;
        CH_TOT[3] = 2;
        F = 'N';




    }


    public void start(){
        IOI = 7;
        MOS();
        simulate();

        while(!ifb.isEmpty() || !TQ.isEmpty() || !ofb.isEmpty() || !IoQ.isEmpty()|| !RQ.isEmpty() || !LQ.isEmpty() || CH[1] || CH[2] || CH[3] || time <6){
            System.out.print("Active channels: ");
            System.out.println(CH[1] + " "+CH[2] + " " + CH[3]);
            System.out.print("Channel timers: ");
            System.out.println(CHT[1] + " "+ CHT[2] + " " + CHT[3]);
            System.out.println("ifb: "+ifb);
            System.out.println("ofb: "+ofb);
            System.out.println("ebq: "+ebq);
            System.out.println(bufferStat);
            System.out.println("IOI = " + IOI);

            if(!LQ.isEmpty()){
                System.out.print("LQ: ");
                for(PCB job : LQ){
                    System.out.print(job.getJobId());
                }
            }
            System.out.println();

            System.out.println();
            if(!RQ.isEmpty()) {
                System.out.print("RQ: ");
                for (PCB job : RQ) {
                    System.out.print(job.getJobId());
                }
            }
            System.out.println();
            if(!IoQ.isEmpty()) {
                System.out.print("IoQ: ");
                for (PCB job : IoQ) {
                    System.out.print(job.getJobId());
                }
            }
            System.out.println();
            if(!TQ.isEmpty()){
                System.out.print("TQ: ");
                for(PCB job : TQ){
                    System.out.print(job.getJobId());
                }
            }

            System.out.println();
            System.out.println("\ntime: "+time);

            System.out.println("SI: " + SI + " PI: " + PI + " TI: "+ TI);

            System.out.println("Used drum frames:");
            for(int i = 0;i<50;i++){
                System.out.print(used_frames_drum[i]);
            }
            System.out.println();

            System.out.println("Used ram frames:");
            for(int i = 0;i<30;i++){
                System.out.print(used_frames_ram[i]);
            }
            System.out.println();

            executeUserProgram();
            simulate();
            MOS();

        }
    }


    public void MOS(){

        if(SI == 0 && PI !=0) {
            switch (PI) {
                case 1:
                    if (TI == 0 || TI == 1) {
                        RQ.peek().terminate_stat = 4;
                        TQ.add(RQ.poll());

                    } else {
                        RQ.peek().terminate_stat = 3;
                        TQ.add(RQ.poll());
                    }
                    break;
                case 2:
                    if (TI == 0 || TI == 1)  {
                        RQ.peek().terminate_stat = 5;
                        TQ.add(RQ.poll());
                    } else {
                        RQ.peek().terminate_stat = 3;
                        TQ.add(RQ.poll());
                    }
                    break;
                case 3:
                    if (TI == 0) {
                        System.out.println("page fault");
                        if (IR[0] == 'G' && IR[1] == 'D' || (IR[0] == 'S' && IR[1] == 'R')) {
                            System.out.println("Resolving..");
                            Allocate_ram();
                            pcb.TTC++;
                        } else {
                            RQ.peek().terminate_stat = 6;
                            TQ.add(RQ.poll());
                        }

                    }

            }

            PI = 0;

            return;

        }
        else if(SI!=0) {
            switch (SI) {
                case 1:
                    if (TI == 0 || TI == 1){
                        System.out.println("REad IOQ");
                        IoQ.add(RQ.poll());
                    }
                    else{
                        RQ.peek().terminate_stat = 3;
                        TQ.add(RQ.poll());
                    }
                    break;

                case 2:
                    if (TI == 0|| TI ==1) {
                        IoQ.add(RQ.poll());
                    }
                    else{
                        RQ.peek().terminate_stat = 3;
                        TQ.add(RQ.poll());      //write then terminate will be checked later;
                    }
                    break;
                case 3:
                    RQ.peek().terminate_stat = 0;
                    TQ.add(RQ.poll());
                    break;
            }
            SI = 0;
            return;
        }
        else if(TI!=0){
            if(TI == 2 && !RQ.isEmpty()){
                RQ.peek().terminate_stat = 3;
                TQ.add(RQ.poll());
            }
        }
        switch (IOI){
            case 0:
                return;
            case 1:
                IR1();
                break;
            case 2:
                IR2();
                break;
            case 3:
                IR2();
                IR1();
                break;
            case 4:
                IR3();
                break;
            case 5:
                IR1();
                IR3();
                break;
            case 6:
                IR3();
                IR2();
                break;
            case 7:
                IR2();
                IR1();
                IR3();
                break;


        }

    }

    public void IR1(){
        try {
            if(reader.ready()) {
                char[] temp;
                if(!ebq.isEmpty()){
                    int index = ebq.poll();
                    temp = reader.readLine().toCharArray();
                    for (int i = 0; i < temp.length && i < 40; i++) {
                        supervisoryStorage[index][i] = temp[i];
                    }
                    startChannel(1);
                    if (supervisoryStorage[index][0] == '$' && supervisoryStorage[index][1] == 'A' && supervisoryStorage[index][2] == 'M' && supervisoryStorage[index][3] == 'J') {
                        System.out.println("AMJ detected, initializing PCB");
                        pcb = new PCB();
                        pcb.setJobId(((supervisoryStorage[index][4] - '0') * 1000) + ((supervisoryStorage[index][5] - '0') * 100) + ((supervisoryStorage[index][6] - '0') * 10) + ((supervisoryStorage[index][7] - '0')));
                        pcb.setTTL(((supervisoryStorage[index][8] - '0') * 1000) + ((supervisoryStorage[index][9] - '0') * 100) + ((supervisoryStorage[index][10] - '0') * 10) + ((supervisoryStorage[index][11] - '0')));
                        pcb.setTLL(((supervisoryStorage[index][12] - '0') * 1000) + ((supervisoryStorage[index][13] - '0') * 100) + ((supervisoryStorage[index][14] - '0') * 10) + ((supervisoryStorage[index][15] - '0')));
                        while(used_frames_drum[pcb.PTR_drum = random.nextInt(50)] != 0){}
                        used_frames_drum[pcb.PTR_drum] = 1;
                        pcb.PTR_drum*=10;
                        pcb.Pcards = 0;
                        pcb.Ptrack = pcb.PTR_drum;
                        System.out.println("Job id: " + pcb.getJobId());
                        System.out.println("TTL: " + pcb.getTTL());
                        System.out.println("TLL: " + pcb.getTLL());
                        System.out.println("Ptrack: " + pcb.Ptrack);
                        System.out.println("Dtrack: " + pcb.Dtrack);
                        buffer_reset(index);
                        ebq.add(index);

                        F = 'P';
                    }
                    else if (supervisoryStorage[index][0] == '$' && supervisoryStorage[index][1] == 'D' && supervisoryStorage[index][2] == 'T' && supervisoryStorage[index][3] == 'A') {
                        System.out.println("DTA detected...");

                        pcb.Dcards = 0;
                        pcb.Dtrack = pcb.PTR_drum + pcb.Pcards;
                        System.out.println("Dtrack " + pcb.Dtrack);
                        buffer_reset(index);

                        ebq.add(index);
                        F = 'D';
                    }
                    else if (supervisoryStorage[index][0] == '$' && supervisoryStorage[index][1] == 'E' && supervisoryStorage[index][2] == 'N' && supervisoryStorage[index][3] == 'D') {
                        buffer_reset(index);
                        ebq.add(index);
                        System.out.println("END detected, putting job "+ pcb.getJobId()+" in load queue");
                        LQ.add(pcb);

                    } else {
                        if (F == 'P') {
                            System.out.println("Program card incoming");
                            System.out.println(supervisoryStorage[index]);
                            pcb.Pcards++;
                            bufferStat[index] = 'P';
                            ifb.add(index);

                        } else if (F == 'D') {
                            System.out.println("Data card incoming");
                            System.out.println(supervisoryStorage[index]);
                            pcb.Dcards++;
                            bufferStat[index] = 'D';
                            ifb.add(index);

                        }


                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error at file reading");
        }
    }


    public void IR2(){
        if(!ofb.isEmpty()){
            System.out.println("CHannel 2 triggered");
            if(!ofb.isEmpty()){
                int index = ofb.poll();
                StringBuffer sb = new StringBuffer();
                int buffer_ptr4 = 0;
//                System.out.println(buffer);
                while(buffer_ptr4 < 40){
                    if(supervisoryStorage[index][buffer_ptr4] != '@')
                        sb.append(supervisoryStorage[index][buffer_ptr4]);
                    buffer_ptr4++;
                }
                sb.append('\n');
                try {
                    Files.write(Paths.get("src/com/company/out3.txt"), String.valueOf(sb).getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                buffer_reset(index);
                ebq.add(index);
                startChannel(2);
            }
        }

    }


    public void IR3(){
        int index;
        int ram_add = 0;// for GD instruction

        switch (task[0]){
            case 'O':
                if(ebq.isEmpty()){
                    System.out.println("Output spooling halted because no empty buffer...");
                    return;
                }
                index = ebq.poll();
                //TODO: check write then terminate i.e when time limit error, then no need to follow second if, if line limit error, then follow second if
                if(job.loadedOcards == job.Olines || job.terminate_stat == 3) {
                    char[] em;
                    if (!ebq.isEmpty()) {
                        switch (job.terminate_stat) {
                            case 0:
                                em = ("\nTerminated Successfully\n\n").toCharArray();
                                for (int i = 0; i < em.length; i++) {
                                    supervisoryStorage[index][i] = em[i];
                                }
                                break;
                            case 1:
                                em = ("\nOut of Data \n\n").toCharArray();
                                for (int i = 0; i < em.length; i++) {
                                    supervisoryStorage[index][i] = em[i];
                                }
                                break;
                            case 2:
                                em = ("\nLine limit exceeded\n\n").toCharArray();
                                for (int i = 0; i < em.length; i++) {
                                    supervisoryStorage[index][i] = em[i];
                                }
                                break;
                            case 3:
                                em = ("\nTime Limit Exceeded\n\n").toCharArray();
                                for (int i = 0; i < em.length; i++) {
                                    supervisoryStorage[index][i] = em[i];
                                }
                                break;
                            case 4:
                                em = ("\nOP code error\n\n").toCharArray();
                                for (int i = 0; i < em.length; i++) {
                                    supervisoryStorage[index][i] = em[i];
                                }
                                break;
                            case 5:
                                em = ("\nOperand Error\n\n").toCharArray();
                                for (int i = 0; i < em.length; i++) {
                                    supervisoryStorage[index][i] = em[i];
                                }
                                break;
                            case 6:
                                em = ("\nInvalid Page fault\n\n").toCharArray();
                                for (int i = 0; i < em.length; i++) {
                                    supervisoryStorage[index][i] = em[i];
                                }
                                break;
                        }
                        ofb.add(index);

                        //saving the state of the system then clearing the drum and resetting the cpu for the next task
                        String jobDetails = "\n\nJobID: " + job.getJobId() + "\tTTL: " + job.getTTL() + "\tTLL: " + job.getTLL() + "\nTTC: " + job.TTC + "\tLLC: "+ job.LLC+"\nPTR_drum: " + job.PTR_drum + "\tPTR_Ram: "+job.PTR_ram + "\nPtrack: " + job.Ptrack + "\tDtrack: " + job.Dtrack + "\tOtrack: "+job.Otrack;
                        save_state(jobDetails);
                        reset_cpu();

                        System.out.println("CPU reset");
                    }
                    clear_drum(TQ.peek());
                    clear_ram(TQ.poll());

                }
                else {
                    if(!ebq.isEmpty()) {
                        int Otrack = realAddress_drum(job.Otrack + job.loadedOcards);
                        int limit = Otrack + 10;
                        int buffer_ptr = 0;
                        for (int i = Otrack; i < limit; i++) {
                            for (int j = 0; j < 4; j++) {
                                if (drum[i][j] == '@')
                                    continue;
                                supervisoryStorage[index][buffer_ptr] = drum[i][j];
                                buffer_ptr++;
                            }


                        }
                        ofb.add(index);
                        job.loadedOcards++;
                }
                }

                task[0] = '@';
                task[1] = '@';

                break;

            case 'I':
                System.out.println("Input Spooling");
                index = ifb.poll();
                if(bufferStat[index] == 'P'){
                    int address = Allocate_drum(pcb);
                    load_memory_instructions_drum(supervisoryStorage[index],address);
                    pcb.loadedPcards ++;
                }
                else if(bufferStat[index] == 'D'){
                    pcb.loadedPcards = 0;
                    int address = Allocate_drum(pcb);
                    load_memory_data_Drum(supervisoryStorage[index],address);
                    pcb.loadedDcards++;
                }
                buffer_reset(index);
                bufferStat[index] = 'E';
                ebq.add(index);
                task[0] = '@';task[1] = '@';
                break;
            case 'L':
                System.out.println("Loading for job: " + job.getJobId());
                if(job.PTR_ram==null){
                    while(used_frames_ram[job.PTR_ram = random.nextInt(30)] != 0){}
                    used_frames_ram[job.PTR_ram] = 1;
                    PTR = job.PTR_ram;
                }

                int page = Allocate_ram();
                int track = realAddress_drum(job.Ptrack + job.loadedPcards);
                int limit = track+10;
                for(int i = track;i<limit;i++){
                    for(int j = 0;j<4;j++){
                        memory[page + i - track][j] = drum[i][j];
                    }

                }

                job.loadedPcards++;
                if(job.loadedPcards == job.Pcards){
                    RQ.add(LQ.poll());
                    RQ.peek().loadedDcards = 0; //for GD purposes
                }
                task[0] = '@';task[1] = '@';
                break;
            case 'R':
                System.out.println("GD started channel 3");
                System.out.println(job.loadedDcards);
                System.out.println(job.Dcards);
                if(job.loadedDcards == job.Dcards){
                    job.terminate_stat = 1;
                    TQ.add(IoQ.poll());
                    task[0] = '@';task[1] = '@';
                    return;
                }
                ram_add = realAddress((IR[2]-'0') *10);

                limit = ram_add+10;
                System.out.println("Ram add: " + ram_add);
                int drum_add = realAddress_drum(job.PTR_drum +job.Pcards+ job.loadedDcards);
                for(int i = ram_add;i < limit;i++){
                    for(int j = 0;j<4;j++){
                        memory[i][j] = drum[drum_add+ (i - ram_add)][j];
                    }
                }
                job.loadedDcards++;
                RQ.add(IoQ.poll());
                task[0] = '@';task[1] = '@';
                break;
            case 'W':
                System.out.println("PD in channel 3");

                drum_add = Allocate_drum(job);
                ram_add = realAddress((IR[2]-'0') *10);
                limit = ram_add+10;
                for(int i = ram_add;i < limit;i++){
                    for(int j = 0;j<4;j++){
                        drum[drum_add+ (i - ram_add)][j] = memory[i][j];
                    }
                }

                job.Olines++;
                job.LLC++;
                if(job.LLC>job.getTLL()){
                    job.terminate_stat = 2;
                    task[0] = '@';task[1] = '@';
                    TQ.add(IoQ.poll());
                    return;
                }

                RQ.add(IoQ.poll());
                task[0] = '@';task[1] = '@';
                break;


        }

        if(!TQ.isEmpty()){
            task[0] = 'O'; task[1] = 'S';
            job = TQ.peek();
            startChannel(3);
        }
        else if(!ifb.isEmpty()){
            task[0] = 'I'; task[1] = 'S';
            startChannel(3);

        }
        else if(!LQ.isEmpty()){
            task[0] = 'L'; task[1] = 'D';
            job = LQ.peek();
            startChannel(3);
        }
        else if(!IoQ.isEmpty()){
            if(IR[0] == 'G' && IR[1] == 'D') {
                job = IoQ.peek();


                task[0] = 'R';
                task[1] = 'D';
                startChannel(3);
            }
            else{
                job = IoQ.peek();
                if(job.Otrack == -1){
                    job.Otrack = job.PTR_drum + job.Pcards + job.Dcards;
                    job.Olines = 0;
                }

                task[0] = 'W';
                task[1] = 'T';
                startChannel(3);
            }

        }




    }




    public void executeUserProgram(){
        //loading IR
        if(RQ.isEmpty() || SI!=0 || PI!=0 || TI!=0)
            return;
        PCB job = RQ.peek();
        System.out.println("Job in execution: "+job.getJobId());
        System.out.println("JOB PTR" + job.PTR_ram);
        restoreCPU(job);

        int ra = realAddress(IC);
        while (IC<99 && memory[ra][0] != '@' && SI==0 && PI==0 && TI==0) {
            System.out.println("IC is " + IC);
            int ra2;
            for (int i = 0; i < 4; i++) {
                IR[i] = memory[ra][i];
            }
            IC++;
            job.IC = IC;

            switch (IR[0]) {
                case 'L':
                    if(IR[1] == 'R'){
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3 || PI ==2)
                            return;
                        else{
                            for(int i = 0;i<4;i++){
                                R[i] = memory[ra2][i];
                            }
                            job.R_state = R;
                        }
                        job.TTC++;
                    }
                    else
                        PI = 1;
                    break;
                case 'S':
                    if(IR[1] == 'R'){
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3 || PI ==2)
                            return;
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));

                        for(int i = 0;i<4;i++){
                            memory[ra2][i] = R[i];
                        }
                        job.TTC++;

                    }
                    else
                        PI = 1;
                    break;
                case 'C':
                    if (IR[1] == 'R') {
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3 || PI ==2)
                            return;
                        else{
                            boolean flag = true;
                            for (int i = 0; i < 4; i++) {
                                if (R[i] != memory[ra2][i]) {
                                    flag = false;
                                    toggle = false;
                                    break;
                                }
                            }
                            if(flag){
                                toggle = true;
                            }
                        }
                        job.TTC++;

                    }
                    else{
                        PI = 1;
                    }
                    break;

                case 'B':
                    if(IR[1] == 'T'){
                        if(PI == 3 || PI ==2)
                            return;
                        else{
                            if(toggle){
                                IC = (IR[2] - '0') *10 + (IR[3] - '0');
                                job.IC = IC;
                            }
                        }
                        job.TTC++;

                    }
                    else
                        PI = 1;
                    break;

                case 'G':
                    if (IR[1] == 'D') {
                        realAddress((IR[2]-'0') *10);
                        if(PI == 2){
                            MOS();
                            return;
                        }
                        SI = 1;

                        if(PI == 3){
                            MOS();
                        }
                        job.TTC++;
                        captureCPU(job);
                        return;
                    }
                    else
                        PI = 1;
                    break;
                case 'P':
                    if (IR[1] == 'D') {
                        realAddress((IR[2]-'0') *10);
                        if(PI == 2 || PI == 3){
                            return;
                        }
                        SI = 2;
                        job.TTC++;
                        captureCPU(job);
                        return;
                    }
                    else
                        PI = 1;
                    break;
                case 'H':
                    SI = 3;
                    pcb.TTC++;
                    break;
                default:
                    PI = 1;
                    return;
            }
            for (int i = 0; i < 4; i++) {
                IR[i] = '@';
            }

            ra = realAddress(IC);
            if(job.TTC>job.getTTL()){
                TI = 2;
                return;
            }


        }
    }


    public void captureCPU(PCB job){
        System.arraycopy(R, 0, job.R_state, 0, 4);
        job.toggle_state = toggle;
        job.IC = IC;

    }
    public void restoreCPU(PCB job){
        System.arraycopy(job.R_state,0,R,0,4);
        toggle = job.toggle_state;
        IC = job.IC;
    }

    public void load_memory_instructions_drum(char[] buffer, int address){
        int buffer_ptr2 = 0;
        int limit = address+10;
        while(buffer_ptr2<40 && buffer[buffer_ptr2]!='@' && address<limit){
            for(int i = 0;i<4;i++){
                if(buffer[buffer_ptr2] == '@')
                    break;
                drum[address][i] = buffer[buffer_ptr2];
                if(buffer[buffer_ptr2]=='H'){
                    buffer_ptr2++;
                    break;
                }
                buffer_ptr2++;
            }
            address++;
        }
    }


    public void load_memory_data_Drum(char[] buffer, int location){
        mem_ptr = location;
        if(mem_ptr>=500){
            System.out.println("Memory Overload, quitting...");
            return;
        }
        int buffer_ptr1 = 0;
        while(buffer_ptr1<40 && buffer[buffer_ptr1]!='@'){
            for(int i = 0;i<4;i++){
                if(buffer[buffer_ptr1] == '@')
                    break;
                drum[mem_ptr][i] = buffer[buffer_ptr1];
                buffer_ptr1++;
            }
            mem_ptr++;
        }

    }


    public void buffer_reset(){
        for(int i = 0;i< 10;i++){
            for(int j = 0;j<40;j++){
                supervisoryStorage[i][j] = '@';
            }
        }
    }


    public void buffer_reset(int buff_no){
        for(int i = 0;i<40;i++){
            supervisoryStorage[buff_no][i] = '@';
        }

    }

    public void clear_drum(PCB job){        //takes job as input, clears the contents present in drum of that job
        int ptr = job.PTR_drum;
        int limit = ptr+10;
        for(int i = ptr;i<limit;i++){
            if(drum[i][0] == '@')
                break;
            int page = realAddress_drum(i);
            int pagelength = page+10;
            for(int j = page;j<pagelength;j++){
                for(int k = 0;k<4;k++){
                    drum[j][k] = '@';
                }
            }
            used_frames_drum[page/10] = 0;
            drum[i][0] = '@';
            drum[i][1] = '@';
            drum[i][2] = '@';
            drum[i][3] = '@';
        }
        used_frames_drum[job.PTR_drum/10] = 0;

        System.out.println("Tracks released for job "+ job.getJobId());

    }

    public void clear_ram(PCB job){        //takes job as input, clears the contents present in ram of that job
        int ptr = job.PTR_ram * 10;
        int limit = ptr+10;

        for(int i = ptr;i<limit;i++){
            if(memory[i][0] == '@')
                break;
            int page;
            page = (memory[i][1]-'0') * 100 + (memory[i][2] - '0')*10 + (memory[i][3] - '0');


            int pagelength = page+10;
            for(int j = page;j<pagelength;j++){
                for(int k = 0;k<4;k++){
                    memory[j][k] = '@';
                }
            }
            used_frames_ram[page/10] = 0;
            memory[i][0] = '@';
            memory[i][1] = '@';
            memory[i][2] = '@';
            memory[i][3] = '@';
        }
        used_frames_ram[job.PTR_ram/10] = 0;


    }


    private int Allocate_drum(PCB job){
        int temp2;
        while(used_frames_drum[temp2 = random.nextInt(50)] != 0){}
        used_frames_drum[temp2] = 1;
        temp2 = temp2 *10;
        int ret = temp2;
        int i = job.PTR_drum;

        while(drum[i][0]!='@'){i++;}


        drum[i][0] = '0';
        drum[i][3] = (char) (temp2 % 10 + '0');
        temp2 /= 10;
        drum[i][2] = (char) (temp2 % 10 + '0');
        temp2 /= 10;
        drum[i][1] = (char) (temp2 % 10 + '0');
        return ret;


    }


    private int Allocate_ram(){
        int temp2;
        while(used_frames_ram[temp2 = random.nextInt(30)] != 0){}
        used_frames_ram[temp2] = 1;
        temp2 = temp2 *10;
        int ret = temp2;
        int i = PTR*10;
        if(IR[2] != '@')
            i = getPTE((IR[2]-'0') *10);
        else{
            while(memory[i][0]!='@'){i++;}
        }


        memory[i][0] = '0';
        memory[i][3] = (char) (temp2 % 10 + '0');
        temp2 /= 10;
        memory[i][2] = (char) (temp2 % 10 + '0');
        temp2 /= 10;
        memory[i][1] = (char) (temp2 % 10 + '0');
        return ret;


    }



    private int realAddress_drum(int VA){       //takes PTR + whatever, returns the int address
        int ra = drum[VA][3] - '0' + (drum[VA][2]-'0') *10 + (drum[VA][1]-'0') *100;
        return ra;
    }

    private int realAddress(int VA){
        if(IR[2]!='@'){
            if((IR[2]-'0'>9) || (IR[2]-'0'<0) || (IR[3]-'0'>9) || (IR[3]-'0'<0)){
                PI = 2;
                return -1;
            }
        }

        if(VA>99 || VA<0){
            PI = 2;
            return -1;
        }

        int pte = getPTE(VA);
        if(memory[pte][0] == '@') {
            PI = 3;
            System.out.println("PI = 3");
            return -1;
        }
        int ra = (memory[pte][1]-'0') * 100 + (memory[pte][2] - '0')*10 + (memory[pte][3] - '0') + VA%10; //real address
        //operand errors

        return ra;
    }

    private int getPTE(int VA){
        return (PTR*10) + VA/10;
    }


    public void startChannel(int i){
        CH[i] = true;
        CHT[i] = 0;
        if(i == 1){
            IOI -=1;
        }
        else if(i == 2){
            IOI -= 2;
        }
        else if(i == 3){
            IOI -=4;
        }
        if(IOI<0)
            IOI = 0;
    }

    public void simulate(){

        for(int i = 1;i<4;i++){
            if(CH[i]){
                CHT[i]++;
                if(CHT[i] == CH_TOT[i]){
                    CH[i] = false;
                    CHT[i] = 0;
                    if(i == 1)
                        IOI += 1;
                    if(i == 2)
                        IOI += 2;
                    if(i == 3)
                        IOI += 4;
                }
            }

        }
        if(IOI>7){
            IOI = 7;
        }
        time++;
    }
    private void reset_cpu(){

        for(int i = 0;i<4;i++){
            IR[i] = '@';
            R[i] = '@';
        }
        IC = 0;
        toggle = false;
        SI = 0;
        EM = 0;
        TI = 0;
        PI = 0;
    }
    private void save_state(String jobDetails){
        String interrupts = "\nSI: "+SI+"\tPI: "+PI+"\tTI: "+TI+"\n";
        String cpuDetails = "\nIC: "+IC+"\nIR: "+IR[0]+IR[1]+IR[2]+IR[3] + "\nR: "+R[0]+R[1]+R[2]+R[3] + "\nToggle: "+toggle+"\n";
        try {
            Files.write(Paths.get("src/com/company/state.txt"), (jobDetails + interrupts + cpuDetails + "\n\n\n").getBytes(), StandardOpenOption.APPEND);

            Files.write(Paths.get("src/com/company/state.txt"), ("Drum\n\n").getBytes(), StandardOpenOption.APPEND);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 500; i++) {
                sb.append(i+ ":");
                for (int j = 0; j < 4; j++) {
                    sb.append(drum[i][j]);
                }
                sb.append("\n");
            }
            Files.write(Paths.get("src/com/company/state.txt"), (sb.toString()).getBytes(), StandardOpenOption.APPEND);


            Files.write(Paths.get("src/com/company/state.txt"), ("Main memory\n\n").getBytes(), StandardOpenOption.APPEND);
            sb = new StringBuffer();
            for (int i = 0; i < 300; i++) {
                sb.append(i+ ":");
                for (int j = 0; j < 4; j++) {
                    sb.append(memory[i][j]);
                }
                sb.append("\n");
            }
            Files.write(Paths.get("src/com/company/state.txt"), (sb.toString()).getBytes(), StandardOpenOption.APPEND);

        }catch (IOException e) {
            e.printStackTrace();
        }

    }



    public void printer(){
        System.out.println("Buffer is: ");
        System.out.println(buffer);
        System.out.println("IC is:"+IC);
        System.out.println("PTR: "+PTR );
        System.out.println("IR:");
        System.out.println(IR);
        System.out.println("R:");
        System.out.println(R);
        System.out.println("Toggle:");
        System.out.println(toggle);
        System.out.println("TTC: "+pcb.TTC);
        System.out.println("LLC: "+pcb.LLC);
        System.out.println("TTL: "+pcb.getTTL());
        System.out.println("TLL: "+pcb.getTLL());

        System.out.println("Used frames are: ");
        for(int i = 0;i<30;i++){
            System.out.print(used_frames_ram[i] );
        }
        System.out.println("\nMain memory");
        for(int i = 0; i< 300;i++){
            System.out.print(i+" : ");
            for(int j = 0;j<4;j++){

                System.out.print(memory[i][j]);
            }
            System.out.println();
        }


    }

}


