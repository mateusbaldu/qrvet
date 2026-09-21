import fs from 'node:fs';
import path from 'node:path';

const out = path.resolve('figma-design/screens');
fs.mkdirSync(out, { recursive: true });

const C = { dark:'#0D443D', green:'#226A54', mint:'#CFF3B8', bg:'#F4F8F6', ink:'#0B302D', gray:'#697783', line:'#D9E1DC', white:'#FFFFFF', pale:'#E8F4EE', amber:'#F5A623', red:'#D85C5C' };
const esc = s => String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;');
const text = (x,y,s,size=16,weight=400,fill=C.ink,anchor='start') => `<text x="${x}" y="${y}" font-family="DM Sans,Arial,sans-serif" font-size="${size}" font-weight="${weight}" fill="${fill}" text-anchor="${anchor}">${esc(s)}</text>`;
const rect = (x,y,w,h,fill=C.white,r=14,stroke='none',sw=1) => `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
const line = (x1,y1,x2,y2,stroke=C.line,sw=1) => `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${stroke}" stroke-width="${sw}"/>`;
const circle = (x,y,r,fill=C.green,stroke='none',sw=1) => `<circle cx="${x}" cy="${y}" r="${r}" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>`;
const button = (x,y,w,label,primary=true) => rect(x,y,w,46,primary?C.green:C.white,10,primary?'none':C.line)+text(x+w/2,y+29,label,14,700,primary?C.white:C.green,'middle');
const field = (x,y,w,label,value='') => text(x,y,label,13,600,C.gray)+rect(x,y+10,w,48,C.white,9,C.line)+text(x+16,y+40,value||label,14,400,value?C.ink:'#9AA6A1');
const badge = (x,y,label,kind='green') => { const fill=kind==='green'?C.mint:kind==='amber'?'#FFF0CE':'#E8EDF0'; const fg=kind==='green'?C.dark:kind==='amber'?'#8A5A00':C.gray; const w=label.length*7.4+24; return rect(x,y,w,28,fill,14)+text(x+w/2,y+19,label,12,700,fg,'middle'); };
const shell = (title,body,active='Pacientes') => `
  ${rect(0,0,1440,900,C.bg,0)}
  ${rect(0,0,246,900,C.dark,0)}
  ${circle(38,40,18,C.mint)}${text(38,46,'+',22,800,C.dark,'middle')}${text(67,42,'QRVet',25,800,C.white)}${text(67,62,'Gestão veterinária',11,400,'#A9C8C0')}
  ${text(28,112,'NAVEGAÇÃO',10,700,'#86AAA1')}
  ${nav(28,132,'⌂','Início',active==='Início')}${nav(28,186,'○','Pacientes',active==='Pacientes')}${nav(28,240,'+','Internações',active==='Internações')}${nav(28,294,'◇','Equipe',active==='Equipe')}${nav(28,348,'⚙','Meu perfil',active==='Perfil')}
  ${circle(45,830,18,'#37675F')}${text(45,835,'MS',10,700,C.white,'middle')}${text(72,826,'Marina Silva',13,700,C.white)}${text(72,844,'Administradora',11,400,'#A9C8C0')}
  ${text(286,46,'Clínica Veterinária',12,700,C.green)}${text(286,78,title,30,800,C.ink)}
  ${button(1202,32,198,'Consultar por QR',false)}
  ${body}`;
const nav=(x,y,ico,label,on)=>`${on?rect(x-10,y-13,210,43,'#1D5B52',10):''}${text(x,y+13,ico,18,700,on?C.mint:'#A9C8C0')}${text(x+30,y+12,label,14,on?700:500,on?C.white:'#C6D9D4')}`;
const svg = (content,w=1440,h=900) => `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><rect width="100%" height="100%" fill="${C.bg}"/>${content}</svg>`;
const save=(name,content,w,h)=>fs.writeFileSync(path.join(out,name),svg(content,w,h));

save('01-login.svg',`
 ${rect(0,0,500,900,C.dark,0)}${circle(56,54,21,C.mint)}${text(56,61,'+',27,800,C.dark,'middle')}${text(90,56,'QRVet',30,800,C.white)}${text(90,78,'Gestão de internação veterinária',12,400,'#A9C8C0')}
 ${rect(52,150,206,32,'#1E5D53',16)}${text(155,171,'FEITO PARA QUEM CUIDA',11,700,C.mint,'middle')}
 ${text(52,253,'O cuidado começa',38,700,C.white)}${text(52,301,'com uma equipe',38,700,C.white)}${text(52,349,'conectada.',38,800,C.mint)}
 ${circle(250,610,112,'none','#2D675F',2)}${circle(250,610,72,'none','#3C766D',2)}${circle(250,610,34,C.mint)}${text(250,620,'+',34,800,C.dark,'middle')}
 ${text(745,216,'Os pacientes estão à espera.',36,800,C.ink)}${text(745,249,'Entre com sua conta para acessar',16,400,C.gray)}
 ${field(745,315,440,'Seu e-mail','admin@qrvet.com')}${field(745,415,440,'Sua senha','••••••••••')}${text(1185,494,'Esqueci minha senha',13,700,C.green,'end')}
 ${button(745,530,440,'Entrar na minha conta  →',true)}${line(745,613,1185,613)}${text(965,650,'Primeiro acesso?',13,700,C.ink,'middle')}${text(965,675,'Use o convite enviado ao seu e-mail pelo administrador.',13,400,C.gray,'middle')}
`,1440,900);

save('02-pacientes.svg',shell('Pacientes',`
 ${text(286,116,'Gerencie os pacientes cadastrados na clínica.',15,400,C.gray)}${button(1160,104,240,'+  Cadastrar paciente',true)}
 ${rect(286,180,1114,82,C.white,14,C.line)}${field(310,198,550,'Buscar','Buscar por paciente ou tutor')}${badge(902,213,'Todos')}${badge(980,213,'Internados','gray')}${badge(1087,213,'Alta','gray')}
 ${rect(286,288,1114,470,C.white,14,C.line)}${text(314,329,'PACIENTE',11,700,C.gray)}${text(620,329,'TUTOR',11,700,C.gray)}${text(880,329,'STATUS',11,700,C.gray)}${text(1120,329,'ÚLTIMA ATUALIZAÇÃO',11,700,C.gray)}${line(310,348,1376,348)}
 ${patientRow(376,'TH','Thor','Cão · Golden Retriever','Marina Souza','Internado','Agora')}${patientRow(466,'LU','Luna','Gato · SRD','Carlos Mendes','Em observação','Há 18 min')}${patientRow(556,'ME','Mel','Cão · Shih-tzu','Bianca Lima','Alta','Ontem, 16:42')}${patientRow(646,'TO','Tobias','Gato · Siamês','Rafael Alves','Alta','02/09/2026')}
`));

function patientRow(y,initial,name,sub,tutor,status,when){return `${circle(338,y,24,C.pale)}${text(338,y+5,11,700,C.green,'middle')}${text(376,y-3,name,15,700,C.ink)}${text(376,y+19,sub,12,400,C.gray)}${text(620,y+5,tutor,14,500,C.ink)}${badge(880,y-14,status,status==='Internado'?'green':status==='Em observação'?'amber':'gray')}${text(1120,y+5,when,13,500,C.gray)}${text(1356,y+5,'›',24,400,C.green,'middle')}${line(310,y+43,1376,y+43)}`;}

save('03-cadastrar-paciente.svg',shell('Cadastrar paciente',`
 ${text(286,116,'Inclua os dados do paciente e de seu responsável.',15,400,C.gray)}
 ${rect(286,158,1114,616,C.white,14,C.line)}${text(318,202,'Dados do paciente',19,800,C.ink)}${line(318,220,1368,220)}
 ${field(318,254,500,'Nome do paciente','Thor')}${field(868,254,500,'Espécie','Cão')}${field(318,342,500,'Raça','Golden Retriever')}${field(868,342,240,'Sexo','Macho')}${field(1128,342,240,'Nascimento','12/04/2021')}
 ${field(318,430,240,'Peso','31,4 kg')}${field(578,430,790,'Observações','Paciente dócil; prefere manejo calmo.')}
 ${text(318,544,'Dados do tutor',19,800,C.ink)}${line(318,562,1368,562)}${field(318,590,500,'Nome do tutor','Marina Souza')}${field(868,590,500,'Telefone','(11) 98888-4455')}${field(318,678,1050,'E-mail','marina.souza@email.com')}
 ${button(986,804,170,'Cancelar',false)}${button(1172,804,196,'Salvar paciente',true)}
`));

save('04-detalhes-paciente.svg',shell('Thor',`
 ${text(286,116,'Prontuário do paciente',15,400,C.gray)}${button(1172,104,228,'+  Abrir internação',true)}
 ${rect(286,170,1114,164,C.white,14,C.line)}${circle(352,244,46,C.pale)}${text(352,253,'TH',20,800,C.green,'middle')}${text(420,218,'Thor',28,800,C.ink)}${text(420,246,'Cão · Golden Retriever · Macho',14,400,C.gray)}${badge(420,268,'Paciente ativo')}
 ${text(760,207,'TUTOR',11,700,C.gray)}${text(760,234,'Marina Souza',15,700,C.ink)}${text(760,260,'(11) 98888-4455',13,400,C.gray)}${text(1040,207,'DADOS CLÍNICOS',11,700,C.gray)}${text(1040,234,'31,4 kg · 5 anos',15,700,C.ink)}${text(1040,260,'Sem alergias conhecidas',13,400,C.gray)}
 ${rect(286,360,546,330,C.white,14,C.line)}${text(318,402,'Histórico recente',19,800,C.ink)}${timeline(318,454,'Consulta de rotina','Vacinas em dia · Dra. Sarah','28/08/2026')}${timeline(318,534,'Exame laboratorial','Hemograma completo','15/07/2026')}${timeline(318,614,'Atendimento','Dermatite controlada','03/05/2026')}
 ${rect(854,360,546,330,C.white,14,C.line)}${text(886,402,'Informações do paciente',19,800,C.ink)}${labelValue(886,452,'Microchip','963008000451207')}${labelValue(886,510,'Dieta habitual','Ração premium · 2x ao dia')}${labelValue(886,568,'Observações','Dócil; prefere manejo calmo')}${labelValue(886,626,'Cadastro','08/09/2026')}
`));

function timeline(x,y,title,sub,date){return `${circle(x+10,y-4,7,C.green)}${line(x+10,y+6,x+10,y+54,'#BED8D0',2)}${text(x+34,y,title,14,700,C.ink)}${text(x+34,y+22,sub,12,400,C.gray)}${text(x+480,y,date,12,600,C.gray,'end')}`}
function labelValue(x,y,l,v){return `${text(x,y,l.toUpperCase(),10,700,C.gray)}${text(x,y+24,v,14,600,C.ink)}`}

save('05-abrir-internacao.svg',shell('Abrir internação',`
 ${text(286,116,'Thor · Crie um novo acompanhamento hospitalar.',15,400,C.gray)}
 ${rect(286,158,1114,616,C.white,14,C.line)}${text(318,202,'Dados da internação',19,800,C.ink)}${line(318,220,1368,220)}
 ${field(318,254,500,'Motivo da internação','Gastroenterite aguda')}${field(868,254,500,'Diagnóstico inicial','Desidratação moderada')}${field(318,342,500,'Veterinário responsável','Dra. Sarah Jenkins')}${field(868,342,240,'Leito','A-04')}${field(1128,342,240,'Entrada','08/09/2026 14:30')}
 ${field(318,430,500,'Dieta','Pastosa gastrointestinal')}${field(868,430,500,'Alergias','Nenhuma conhecida')}${field(318,518,1050,'Atenção especial','Monitorar hidratação e episódios de vômito')}${field(318,606,1050,'Observações','Paciente estável, consciente e responsivo.')}
 ${button(964,804,192,'Salvar rascunho',false)}${button(1172,804,196,'Iniciar internação',true)}
`,'Internações'));

save('06-internacao-ativa.svg',shell('Internação de Thor',`
 ${badge(286,104,'Em observação','amber')}${text(286,148,'Entrada em 08/09/2026 às 14:30 · Leito A-04 · Dra. Sarah Jenkins',14,400,C.gray)}
 ${button(932,104,220,'Registrar alimentação',false)}${button(1168,104,232,'Registrar medicação',true)}
 ${rect(286,188,730,602,C.white,14,C.line)}${text(318,228,'Linha do tempo',20,800,C.ink)}${text(318,252,'Atualizações do acompanhamento hospitalar',13,400,C.gray)}
 ${timelineCard(318,290,'15:42','Medicação administrada','Ondansetrona · 0,5 mg/kg · IV','Dra. Sarah Jenkins',C.green)}${timelineCard(318,410,'15:10','Alimentação — aceitação boa','Ração pastosa · 120 g oferecidos · 90 g consumidos','Mike Ross',C.amber)}${timelineCard(318,530,'14:30','Internação iniciada','Paciente estável, consciente e responsivo.','Dra. Sarah Jenkins',C.green)}
 ${rect(1040,188,360,286,C.white,14,C.line)}${text(1072,228,'Acesso do tutor',19,800,C.ink)}${qr(1120,256,136)}${text(1208,420,'qrvet.app/i/TH-9248',12,600,C.gray,'middle')}${button(1072,438,140,'Copiar link',false)}${button(1224,438,144,'Baixar QR',false)}
 ${rect(1040,498,360,292,C.white,14,C.line)}${text(1072,538,'Resumo clínico',19,800,C.ink)}${labelValue(1072,582,'Diagnóstico','Desidratação moderada')}${labelValue(1072,640,'Dieta','Pastosa gastrointestinal')}${labelValue(1072,698,'Última atualização','Agora · Dra. Sarah')}
`,'Internações'));

function timelineCard(x,y,time,title,sub,by,color){return `${circle(x+12,y+12,12,color)}${text(x+48,y+7,time,11,700,C.gray)}${text(x+48,y+32,title,15,700,C.ink)}${text(x+48,y+56,sub,13,400,C.gray)}${text(x+48,y+78,by,11,600,C.green)}${line(x+12,y+26,x+12,y+102,'#BED8D0',2)}`}
function qr(x,y,s){let o=rect(x,y,s,s,C.white,4,C.line); const cell=s/13; for(let r=0;r<13;r++)for(let c=0;c<13;c++){if(((r*c+r+c*3)%5<2)||((r<4||r>8)&&(c<4||c>8)))o+=rect(x+c*cell+2,y+r*cell+2,cell-3,cell-3,C.ink,0);}return o;}

save('07-consultar-qr.svg',shell('Consultar internação',`
 ${text(286,116,'Acesse o acompanhamento público usando o código do QR Code.',15,400,C.gray)}
 ${rect(458,190,770,480,C.white,18,C.line)}${circle(843,260,42,C.pale)}${text(843,271,'⌗',34,700,C.green,'middle')}${text(843,334,'Consulte uma internação',27,800,C.ink,'middle')}${text(843,365,'Digite o código exibido no QR Code entregue pela clínica.',14,400,C.gray,'middle')}
 ${field(598,410,490,'Código da internação','TH-9248')}${button(598,510,490,'Consultar acompanhamento  →',true)}${text(843,594,'O tutor verá somente informações autorizadas.',12,500,C.gray,'middle')}
`,'Internações'));

save('08-publico-mobile.svg',`
 ${rect(0,0,390,844,C.bg,0)}${rect(0,0,390,94,C.dark,0)}${circle(28,34,14,C.mint)}${text(28,39,'+',18,800,C.dark,'middle')}${text(52,39,'QRVet',20,800,C.white)}${text(52,58,'Acompanhamento do tutor',10,400,'#A9C8C0')}
 ${text(24,132,'Thor',28,800,C.ink)}${text(24,156,'Cão · Golden Retriever',13,400,C.gray)}${badge(244,124,'Em observação','amber')}
 ${rect(20,184,350,112,C.white,14,C.line)}${text(40,218,'Última atualização',11,700,C.gray)}${text(40,246,'Agora, às 15:42',18,800,C.ink)}${text(40,272,'Responsável: Dra. Sarah Jenkins',12,400,C.gray)}
 ${text(24,338,'Como Thor está?',20,800,C.ink)}${rect(20,360,350,88,'#E5F6DD',14)}${circle(50,404,17,C.mint)}${text(50,410,'✓',18,800,C.green,'middle')}${text(80,395,'Estável e sendo acompanhado',14,700,C.dark)}${text(80,418,'Nossa equipe está cuidando dele.',12,400,C.gray)}
 ${text(24,492,'Atualizações',20,800,C.ink)}${mobileEvent(24,532,'15:42','Medicação realizada','Thor recebeu a medicação prevista.')}${mobileEvent(24,626,'15:10','Alimentação','Comeu bem e permanece confortável.')}${mobileEvent(24,720,'14:30','Internação iniciada','Thor foi acomodado e avaliado.')}
`,390,844);
function mobileEvent(x,y,time,title,sub){return `${circle(x+10,y+8,7,C.green)}${line(x+10,y+18,x+10,y+78,'#BED8D0',2)}${text(x+34,y+4,time,11,700,C.green)}${text(x+34,y+28,title,14,700,C.ink)}${text(x+34,y+50,sub,12,400,C.gray)}`}

save('09-modal-alimentacao.svg',shell('Internação de Thor',`
 ${rect(246,0,1194,900,'#0B302D',0)}<rect x="246" width="1194" height="900" fill="#0B302D" opacity="0.48"/>
 ${rect(472,120,740,650,C.white,18)}${text(510,170,'Registrar alimentação',24,800,C.ink)}${text(1174,168,'×',28,400,C.gray,'middle')}${line(510,194,1174,194)}
 ${field(510,230,310,'Data e hora','08/09/2026 15:10')}${field(854,230,320,'Profissional','Mike Ross')}${field(510,320,664,'Alimento','Ração pastosa gastrointestinal')}${field(510,410,310,'Quantidade oferecida','120 g')}${field(854,410,320,'Quantidade consumida','90 g')}${field(510,500,310,'Aceitação','Boa')}${field(854,500,320,'Observações','Aceitou espontaneamente')}
 ${button(802,686,174,'Cancelar',false)}${button(994,686,180,'Registrar',true)}
`,'Internações'));

save('10-modal-medicacao.svg',shell('Internação de Thor',`
 ${rect(246,0,1194,900,'#0B302D',0)}<rect x="246" width="1194" height="900" fill="#0B302D" opacity="0.48"/>
 ${rect(446,80,792,730,C.white,18)}${text(486,130,'Registrar medicação',24,800,C.ink)}${text(1198,128,'×',28,400,C.gray,'middle')}${line(486,154,1198,154)}
 ${field(486,190,350,'Medicamento','Ondansetrona')}${field(860,190,338,'Dose','0,5 mg/kg')}${field(486,280,230,'Unidade','mg/kg')}${field(738,280,220,'Via','Intravenosa')}${field(980,280,218,'Data e hora','08/09 15:42')}${field(486,370,712,'Prescrição','Administrar a cada 8 horas')}${field(486,460,712,'Observações','Sem intercorrências durante a administração')}${field(486,550,712,'Profissional','Dra. Sarah Jenkins')}
 ${rect(486,638,712,52,'#FFF7E5',10)}${text(510,669,'✓ Confirmo que a medicação foi administrada ao paciente.',13,700,'#805B12')}${button(810,724,180,'Cancelar',false)}${button(1008,724,190,'Confirmar',true)}
`,'Internações'));

save('11-equipe.svg',shell('Equipe',`
 ${text(286,116,'Gerencie quem tem acesso à clínica e suas permissões.',15,400,C.gray)}${button(1174,104,226,'+  Convidar membro',true)}
 ${stat(286,'5','Total')}${stat(510,'1','Admins')}${stat(734,'2','Veterinários')}${stat(958,'1','Recepcionistas')}${stat(1182,'1','Pendentes')}
 ${rect(286,276,1114,86,C.white,14,C.line)}${field(310,294,480,'Buscar','Nome ou e-mail')}${badge(830,309,'Todos')}${badge(905,309,'Admins','gray')}${badge(990,309,'Veterinários','gray')}${badge(1102,309,'Recepcionistas','gray')}${badge(1240,309,'Pendentes','gray')}
 ${rect(286,386,1114,386,C.white,14,C.line)}${text(314,425,'MEMBRO',11,700,C.gray)}${text(720,425,'PERMISSÃO',11,700,C.gray)}${text(948,425,'STATUS',11,700,C.gray)}${text(1164,425,'ADICIONADO EM',11,700,C.gray)}${line(310,444,1376,444)}
 ${member(476,'SJ','Dra. Sarah Jenkins','sarah@qrvet.clinic','Admin','Ativo · Agora','14/01/2026')}${member(542,'MR','Mike Ross','mike@qrvet.clinic','Veterinário','Ativo · Há 2h','02/02/2026')}${member(608,'AC','Anna Costa','anna@qrvet.clinic','Veterinário','Inativo','11/03/2026')}${member(674,'CA','Camila Alves','camila@qrvet.clinic','Recepcionista','Ativo · Há 35 min','08/04/2026')}${member(740,'✉','Aguardando aceite','lucas@qrvet.clinic','Veterinário','Convite pendente','17/05/2026')}
`,'Equipe'));
function stat(x,n,l){return `${rect(x,158,202,92,C.white,14,C.line)}${text(x+20,196,n,25,800,C.ink)}${text(x+20,222,l,12,600,C.gray)}`}
function member(y,i,n,email,role,status,date){return `${circle(338,y,20,C.pale)}${text(338,y+4,i,10,700,C.green,'middle')}${text(372,y-3,n,14,700,C.ink)}${text(372,y+17,email,11,400,C.gray)}${badge(720,y-14,role,role==='Admin'?'green':'gray')}${text(948,y+4,status,12,600,status.startsWith('Ativo')?C.green:C.gray)}${text(1164,y+4,date,12,500,C.gray)}${text(1354,y+4,'⋮',20,700,C.gray,'middle')}`}

console.log(`Generated ${fs.readdirSync(out).length} editable SVG screens in ${out}`);
