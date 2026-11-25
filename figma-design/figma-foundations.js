const createdNodeIds=[];
function track(n){createdNodeIds.push(n.id);return n;}
function color(hex){return {r:parseInt(hex.slice(1,3),16)/255,g:parseInt(hex.slice(3,5),16)/255,b:parseInt(hex.slice(5,7),16)/255};}
async function createVariableCollection(name,modeNames){const collection=figma.variables.createVariableCollection(name);const modeIds={};const initial=collection.modes[0];collection.renameMode(initial.modeId,modeNames[0]);modeIds[modeNames[0]]=initial.modeId;for(let i=1;i<modeNames.length;i++)modeIds[modeNames[i]]=collection.addMode(modeNames[i]);return{collection,modeIds};}
const page=figma.root.children[0];await figma.setCurrentPageAsync(page);page.name='01 · Protótipo completo';
const comps=track(figma.createPage());comps.name='02 · Componentes QRVET';
const colors={dark:'#0D443D',primary:'#226A54',accent:'#CFF3B8',background:'#F4F8F6',ink:'#0B302D',muted:'#697783',border:'#D9E1DC',white:'#FFFFFF',pale:'#E8F4EE',warning:'#FFF7E8',warningText:'#8A601A',error:'#FFF0ED',errorText:'#9C3021'};
const {collection:palette}=await createVariableCollection('QRVET · Primitivos',['Padrão']);
const {collection:semantic}=await createVariableCollection('QRVET · Interface',['Padrão']);
const vars={};const prim={};
for(const [key,value] of Object.entries(colors)){const p=figma.variables.createVariable('Color/'+key,palette,'COLOR');p.scopes=[];p.setValueForMode(palette.defaultModeId,color(value));p.setVariableCodeSyntax('WEB',`var(--qrvet-${key})`);prim[key]=p.id;const v=figma.variables.createVariable(key,semantic,'COLOR');v.scopes=['FRAME_FILL','SHAPE_FILL','TEXT_FILL','STROKE_COLOR'];v.setValueForMode(semantic.defaultModeId,{type:'VARIABLE_ALIAS',id:p.id});v.setVariableCodeSyntax('WEB',`var(--qrvet-${key})`);vars[key]=v.id;}
for(const size of [4,8,12,16,20,24,32]){const v=figma.variables.createVariable('space/'+size,semantic,'FLOAT');v.scopes=['GAP'];v.setValueForMode(semantic.defaultModeId,size);v.setVariableCodeSyntax('WEB',`var(--space-${size})`);vars['space'+size]=v.id;}
for(const size of [8,12,16]){const v=figma.variables.createVariable('radius/'+size,semantic,'FLOAT');v.scopes=['CORNER_RADIUS'];v.setValueForMode(semantic.defaultModeId,size);v.setVariableCodeSyntax('WEB',`var(--radius-${size})`);vars['radius'+size]=v.id;}
await Promise.all(['Regular','Medium','SemiBold','Bold'].map(style=>figma.loadFontAsync({family:'DM Sans',style})));
const styles={};for(const [key,size,style] of [['Title',28,'Bold'],['Section',18,'SemiBold'],['Body',14,'Regular'],['Small',12,'Regular'],['Label',13,'SemiBold'],['Button',13,'SemiBold']]){const s=figma.createTextStyle();s.name='QRVET/'+key;s.fontName={family:'DM Sans',style};s.fontSize=size;s.lineHeight={unit:'PERCENT',value:145};styles[key]=s.id;}
for(const id of ['6:2','6:3']){const n=await figma.getNodeByIdAsync(id);if(n){n.name=id==='6:2'?'Foto de referência · Luna':'Foto de referência · Thor';comps.appendChild(n);n.resize(160,160);n.x=id==='6:2'?800:1000;n.y=100;createdNodeIds.push(n.id);}}
return {createdNodeIds,pages:{prototype:page.id,components:comps.id},variables:vars,styles,collections:{primitive:palette.id,semantic:semantic.id}};
