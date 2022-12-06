
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

    //interrupts
    int SI; //system Interrupt
    int TI; //Time interrupt
    int PI; //page interrupt
    Integer EMg; //errror message global;

    //miscellaneous
    int [] used_frames; //keeps track of the frames used
    int temp1;   //temporary usage variable
    Random random;
    PCB pcb; // pcb data structure;

    class PCB{
        private int TTL;
        private int TLL;
        private int jobId;
        int TTC;
        int LLC;
        public PCB(){
            this.TTC = 0;
            this.LLC = 0;
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
            return jobId;
        }

        public int getTLL() {
            return TLL;
        }

        public int getTTL() {
            return TTL;
        }
    }



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
        EMg = null;
        TI = 0;
        PI = 0;

        used_frames = new int[30];




    }


    public void MOS(){
        if(PI != 0){
            switch (PI) {
                case 1:
                    if (TI == 0) {
                        terminate(4);
                    } else {
                        terminate(3);
                        terminate(4);
                    }
                    break;
                case 2:
                    if (TI == 0) {
                        terminate(5);
                    } else {
                        terminate(3);
                        terminate(5);
                    }
                    break;
                case 3:
                    if (TI == 0) {
                        System.out.println("page fault");
                        if (SI == 1 || (IR[0] == 'S' && IR[1] == 'R')) {
                            System.out.println("Resolving..");
                            Allocate();
                            pcb.TTC++;
                        } else {
                            terminate(6);
                        }

                    }
                    if (TI == 2) {
                        terminate(3);
                    }

            }
            PI = 0;



        }
        else {
            switch(SI){
                case 1:
                    if (TI == 0) {
                        try {
                            read();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        terminate(3);
                    }
                    break;

                case 2:
                    if (TI == 0) {
                        write();
                    } else {
                        write();
                        terminate(3);
                    }
                    break;
                case 3:
                    terminate(0);
                    break;

            }
            SI = 0;
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
        if (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {
            System.out.println("Program Over");
            terminate(1);
            return;
        }
        int ra = realAddress((IR[2]-'0') *10);
        if(PI == 2){
            MOS();
            return;
        }
        if(PI == 3){
            MOS();
        }
        ra = realAddress((IR[2]-'0') *10);

        load_memory_data(ra);
        buffer_reset();

    }


    public void write(){
        System.out.println("Write executed");
        IR[3] = '0';
        pcb.LLC++;
        if(pcb.LLC>pcb.getTLL())
            terminate(2);
        else{
            try{
                int memory_ptr = realAddress((IR[2]-'0')*10);
                if(PI == 2 || PI == 3){
                    MOS();
                }
                else{
                    int buffer_ptr4 = 0;
                    int limit = memory_ptr+10;

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
                    Files.write(Paths.get("out.txt"), String.valueOf(sb).getBytes(), StandardOpenOption.APPEND);


                    buffer_reset();

                }
            }
            catch(IOException e){
                System.out.println("Exception occured at write()");
                e.printStackTrace();
            }
        }



    }


    public void terminate(int EM){
        EMg = EM;
        String jobDetails = "\n\nJobID: " + pcb.getJobId() + "\tTTL: " + pcb.getTTL() + "\tTLL: " + pcb.getTLL() + "\nTTC: " + pcb.TTC + "\tLLC: "+ pcb.LLC+"\n";
        String interrupts = "\nSI: "+SI+"\tPI: "+PI+"\tTI: "+TI+"\n";
        String cpuDetails = "\nIC: "+IC+"\nIR: "+IR[0]+IR[1]+IR[2]+IR[3] + "\nR: "+R[0]+R[1]+R[2]+R[3] + "\nToggle: "+toggle+"\n";
        try{
            switch (EM){

                case 0:
                    Files.write(Paths.get("out.txt"), (jobDetails+interrupts+cpuDetails+"\nTerminated Sucessfully\n\n").getBytes(), StandardOpenOption.APPEND);
                    break;
                case 1:
                    Files.write(Paths.get("out.txt"), (jobDetails+interrupts+cpuDetails+"\nOut of Data\n\n").getBytes(), StandardOpenOption.APPEND);
                    break;
                case 2:
                    Files.write(Paths.get("out.txt"), (jobDetails+interrupts+cpuDetails+"\nLine limit exceeded\n\n").getBytes(), StandardOpenOption.APPEND);
                    break;
                case 3:
                    Files.write(Paths.get("out.txt"), (jobDetails+interrupts+cpuDetails+"\nTime Limit Exceeded\n\n").getBytes(), StandardOpenOption.APPEND);
                    break;
                case 4:
                    Files.write(Paths.get("out.txt"), (jobDetails+interrupts+cpuDetails+ "\nOP code error\n\n").getBytes(), StandardOpenOption.APPEND);
                    break;
                case 5:
                    Files.write(Paths.get("out.txt"), (jobDetails+interrupts+cpuDetails+ "\nOperand error\n\n").getBytes(), StandardOpenOption.APPEND);
                    break;
                case 6:
                    Files.write(Paths.get("out.txt"), (jobDetails+interrupts+cpuDetails+"\nInvalid page fault\n\n").getBytes(), StandardOpenOption.APPEND);
                    break;

            }
        }
        catch(IOException e){
            System.out.println("Exception occured at terminate()");
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
        while (IC<99 && memory[ra][0] != '@') {


            System.out.println("IC is " + IC);
            int ra2;
            for (int i = 0; i < 4; i++) {
                IR[i] = memory[ra][i];
            }
            IC++;

            switch (IR[0]) {
                case 'L':
                    if(IR[1] == 'R'){
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3 || PI ==2)
                            MOS();
                        else{
                            for(int i = 0;i<4;i++){
                                R[i] = memory[ra2][i];
                            }
                        }
                        pcb.TTC++;
                    }
                    else{
                        PI = 1;
                        MOS();
                    }
                    break;
                case 'S':
                    if(IR[1] == 'R'){
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3 || PI ==2)
                            MOS();
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));

                        for(int i = 0;i<4;i++){
                            memory[ra2][i] = R[i];
                        }
                        pcb.TTC++;

                    }
                    else{
                        PI = 1;
                        MOS();
                    }
                    break;
                case 'C':
                    if (IR[1] == 'R') {
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3 || PI ==2)
                            MOS();
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
                        pcb.TTC++;

                    }
                    else{
                        PI = 1;
                        MOS();
                    }
                    break;

                case 'B':
                    if(IR[1] == 'T'){
                        ra2 = realAddress((IR[2] - '0')*10 + (IR[3] - '0'));
                        if(PI == 3 || PI ==2)
                            MOS();
                        else{
                            if(toggle){
                                IC = (IR[2] - '0') *10 + (IR[3] - '0');
                            }
                        }
                        pcb.TTC++;

                    }
                    else{
                        PI = 1;
                        MOS();
                    }
                    break;

                case 'G':
                    if (IR[1] == 'D') {
                        SI = 1;
                        MOS();
                        pcb.TTC++;

                    }
                    else{
                        PI = 1;
                        MOS();
                    }
                    break;
                case 'P':
                    if (IR[1] == 'D') {
                        SI = 2;
                        MOS();
                        pcb.TTC++;

                    }
                    else{
                        PI = 1;
                        MOS();
                    }
                    break;
                case 'H':
                    SI = 3;
                    MOS();
                    pcb.TTC++;
                    return;
                default:
                    PI = 1;
                    MOS();
                    return;






            }
            for (int i = 0; i < 4; i++) {
                IR[i] = '@';
            }

            ra = realAddress(IC);
            if(pcb.TTC>=pcb.getTTL()){
                TI = 2;
                MOS();
            }
            if(EMg!=null){
                return;
            }


        }
    }






    private static java.io.File file;
    private static BufferedReader reader;
    static {
        try {
            file = new java.io.File("input.txt");
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
                for (buffer_ptr = 0; buffer_ptr < temp.length && buffer_ptr < 40; buffer_ptr++) {
                    buffer[buffer_ptr] = temp[buffer_ptr];
                }
                System.out.println(buffer);

                if (buffer[0] == '$' && buffer[1] == 'A' && buffer[2] == 'M' && buffer[3] == 'J') {
                    pcb = new PCB();
                    pcb.setJobId(((buffer[4] - '0')*1000) + ((buffer[5] - '0')*100) + ((buffer[6] - '0')*10) + ((buffer[7] - '0')));
                    pcb.setTTL(((buffer[8] - '0')*1000) + ((buffer[9] - '0')*100) + ((buffer[10] - '0')*10) + ((buffer[11] - '0')));
                    pcb.setTLL(((buffer[12] - '0')*1000) + ((buffer[13] - '0')*100) + ((buffer[14] - '0')*10) + ((buffer[15] - '0')));
                    //page allocation
                    System.out.println("Job id: " + pcb.getJobId());
                    System.out.println("TTL: " + pcb.getTTL());
                    System.out.println("TLL: " + pcb.getTLL());

                    PTR = random.nextInt(30);
                    used_frames[PTR] = 1;


                    buffer_reset();
                }
                else if (buffer[0] == '$' && buffer[1] == 'D' && buffer[2] == 'T' && buffer[3] == 'A') {
                    buffer_reset();
                    MOSstartExec();
                }
                else if (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {
                    System.out.println("Program Over");
                    printer();
                    value_allocator();
                }
                else{
                    //instruction frame allocation and loading
                    temp1=Allocate() ;
                    load_memory_instructions(temp1);
                    buffer_reset();
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
        temp2 = temp2 *10;
        int ret = temp2;
        int i = PTR * 10;
        if(IR[2] != '@')
            i = getPTE((IR[2]-'0') *10);
        else{
            while(memory[i][0]!='@'){i++;}
        }
        System.out.println("PTE is " + i);

        memory[i][0] = '0';
        memory[i][3] = (char) (temp2 % 10 + '0');
        temp2 /= 10;
        memory[i][2] = (char) (temp2 % 10 + '0');
        temp2 /= 10;
        memory[i][1] = (char) (temp2 % 10 + '0');
        return ret;


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

        return ra;
    }
    private int getPTE(int VA){
        return (PTR *10) + VA/10;
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
            System.out.print(used_frames[i] );
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


