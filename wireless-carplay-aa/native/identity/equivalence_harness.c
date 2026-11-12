/* ARM host harness: serialize the same EV contribution used by the former EV Routing owner. */
#include "identity.c"
void *dlopen(const char *p,int f){(void)p;(void)f;return 0;}
void *dlsym(void *h,const char *s){(void)h;(void)s;return 0;}
void *memcpy(void *d,const void *s,u32 n){char *a=d;const char *b=s;while(n--)*a++=*b++;return d;}
void *__aeabi_memcpy(void *d,const void *s,u32 n){char *a=d;const char *b=s;while(n--)*a++=*b++;return d;}
void *__aeabi_memcpy4(void *d,const void *s,u32 n){return __aeabi_memcpy(d,s,n);}

char output[3][1024];
int result;
static u32 at,value=42,mailbox,slot;
static Param root[24],stock_info[5],stock_status[13],stock_wireless[2];
static void put(const char *s){while(*s && at<sizeof(output[0])-1)output[slot][at++]=*s++;output[slot][at]=0;}
static void num(u32 v){
    static const u32 powers[]={1000000000,100000000,10000000,1000000,100000,10000,1000,100,10,1};int started=0;
    for(u32 i=0;i<10;i++){u32 digit=0;while(v>=powers[i]){v-=powers[i];digit++;}if(digit || started || i==9){output[slot][at++]=(char)('0'+digit);started=1;}}output[slot][at]=0;
}
static void *stock_lookup(u16 id,u16 type,u16 *n,void *key,int *more){(void)id;(void)type;(void)n;(void)key;if(more)*more=0;return &value;}
static char wireless_name[]="stock";
static void *wireless_values(u16 id,u16 type,u16 *n,void *key,int *more){
    (void)type;(void)n;(void)key;if(more)*more=0;return id==1?(void*)wireless_name:(void*)&value;
}
static void *root_values(u16 id,u16 type,u16 *n,void *key,int *more){
    (void)type;(void)n;(void)key;if(more)*more=0;
    if(id==20 || id==21)return (void*)stock_lookup;if(id==24)return (void*)wireless_values;return &value;
}
static const Param *stock_wireless_children(u16 id,u16 type,u16 *count,void *key,int *more){
    (void)id;(void)type;(void)key;if(more)*more=0;*count=2;return stock_wireless;
}
static void walk(const Param *table,u32 count,Lookup lookup,u16 group){
    for(u32 i=0;i<count;i++){
        const Param *p=&table[i];if(!p->used)continue;
        if(p->type==13){u16 n=0;int more=0;Children children=(Children)p->children;const Param *items=children(p->id,p->type,&n,0,&more);Lookup sub=(Lookup)lookup(p->id,p->type,&n,0,&more);put("[");num(p->id);put(":");walk(items,n,sub,p->id);put("]");continue;}
        if(p->init)((void (*)(u16,u16,u32,void*))p->init)(p->id,p->type,0,0);
        u16 n=0;int more=0;void *v=lookup(p->id,p->type,&n,0,&more);num(group);put(".");num(p->id);put("=");
        if(p->type==10)put((const char*)v);else if(p->type==12)put("");else num(*(u32*)v);put(";");
    }
}
static u32 build(u32 h,u32 s,u32 a,u32 b,u32 cmd,void *table,u32 count,const char *name,Lookup lookup){
    (void)h;(void)s;(void)a;(void)b;(void)cmd;(void)name;at=0;output[slot][0]=0;walk(table,count,lookup,0);return 1;
}
static int mutex(void *p){(void)p;return 0;}
static int protect(void *p,u32 n,int flags){(void)p;(void)n;(void)flags;return 0;}
static void semihost(u32 operation,const void *argument);
int test_main(void){
    root[0]=parameter(20,13);root[0].children=(u32)identity_children;
    root[1]=parameter(21,13);root[1].children=(u32)identity_children;
    root[0].used=root[1].used=0;
    root[2]=parameter(24,13);root[2].children=(u32)stock_wireless_children;
    static const u16 info_ids[5]={0,1,2,6,8};
    for(u32 i=0;i<5;i++)stock_info[i]=parameter(info_ids[i],(info_ids[i]==6 || info_ids[i]==8)?10:8);
    for(u32 i=0;i<13;i++){stock_status[i]=parameter((u16)i,6);stock_status[i].used=0;}
    stock_wireless[0]=parameter(0,5);stock_wireless[1]=parameter(1,10);
    root_table=root;info_table=stock_info;status_table=stock_status;
    root_lookup=root_values;info_lookup=stock_lookup;status_lookup=stock_lookup;wireless_lookup=wireless_values;raw_builder=build;
    lock_mutex=mutex;unlock_mutex=mutex;protect_page=protect;hook_mailbox=&mailbox;
    result=configure("Kia EV6",7,2,0,235000,2,10900,"",0);slot=0;
    identity_builder(0,0,0,0,0x1d01,root,24,"identity",root_values);
    result|=configure("Kia EV6",7,2,0,235000,2,10900,"THEME1:985:19.0",15);slot=1;
    identity_builder(0,0,0,0,0x1d01,root,24,"identity",root_values);
    result|=configure("",0,-1,-1,0,-1,0,"THEME1:985:19.0",15);slot=2;
    identity_builder(0,0,0,0,0x1d01,root,24,"identity",root_values);
    return 0;
}
static const char expected_vehicle[]="[20:20.0=42;20.1=42;20.2=2;20.6=Kia EV6;20.8=Kia EV6;20.11=0;20.11=2;20.12=235000;20.14=10900;][21:21.3=42;21.6=42;21.11=42;21.21=;21.30=;][24:24.0=42;24.1=stock;]";
static const char expected_both[]="[20:20.0=42;20.1=42;20.2=2;20.6=Kia EV6;20.8=Kia EV6;20.11=0;20.11=2;20.12=235000;20.14=10900;][21:21.3=42;21.6=42;21.11=42;21.21=;21.30=;][24:24.0=42;24.1=stock;24.5=;24.6=THEME1:985:19.0;]";
static const char expected_theme[]="[24:24.0=42;24.1=stock;24.5=;24.6=THEME1:985:19.0;]";
static const char pass[]="PASS Wireless serializer output is byte-for-byte equivalent to the former EV Routing serializer\n";
static const char fail[]="FAIL Wireless serializer output differs from the former EV Routing serializer\n";
static const char start[]="Running ARM CarPlay identity comparison\n";
static int same(const char *a,const char *b){while(*a && *a==*b){a++;b++;}return *a==*b;}
static void semihost(u32 operation,const void *argument){
    register u32 r0 __asm__("r0")=operation;register const void *r1 __asm__("r1")=argument;
    __asm__ volatile("svc 0x123456"::"r"(r0),"r"(r1):"memory");
}
void run(void){
    semihost(4,start);test_main();int ok=result==0 && same(output[0],expected_vehicle) && same(output[1],expected_both) && same(output[2],expected_theme);
    if(!ok){semihost(4,output[0]);semihost(4,"\n");semihost(4,output[1]);semihost(4,"\n");semihost(4,output[2]);semihost(4,"\n");}
    semihost(4,ok?pass:fail);
    u32 status[2]={0x20026,ok?0:1};semihost(0x20,status);for(;;){}
}
__attribute__((naked)) void _start(void){__asm__ volatile("ldr sp, =0x01000000\nb run");}
