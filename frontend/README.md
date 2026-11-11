# Frontend QRVet

Execute `npm ci` e `npm run dev` nesta pasta. O Vite atende em http://localhost:5173 e encaminha `/qrvet/api` para o backend em http://localhost:8080. Para outra origem da API, configure `VITE_API_URL` antes da compilação.

## Fluxos disponíveis

- Autenticação, convite, recuperação de senha e perfil.
- Nome editável em Meu perfil, com atualização do cabeçalho e menu após salvar.
- Alertas de alergias, restrições alimentares e manejo em destaque na ficha, internação e cuidados. Edição por administradores e veterinários.
- Evoluções por internação, com horário e autoria preservados, cinco registros por página e histórico disponível após alta. Administradores, veterinários e auxiliares podem registrar durante a internação ativa.
- Equipe e gestão de sessões (administrador).
- Painel inicial com ocupação das baias, internações e resumo das refeições de hoje.
- Cadastro conjunto de paciente e tutor, busca de tutor existente, edição de ambos e atalho para abrir internação.
- Fotos locais: prévia, upload autenticado, troca e remoção. Falhas de upload preservam o cadastro salvo.
- Baias: cadastro, edição e manutenção; ocupação e liberação automáticas durante a internação.
- Internações: abertura com seleção de paciente/baia/veterinário, filtros, histórico, encerramento, QR Code para o vidro da clínica e envio do link de acompanhamento ao tutor por e-mail.
- Alimentação: agenda por data e horários, repetição diária limitada a 31 dias, checklist geral com filtros, conclusão com observação, cancelamento e indicação de pacientes sem horários.
- Cuidados: agenda individual, histórico de alimentação e início/fim de jejum. O auxiliar escolhe um paciente no diretório de cuidados, sem acessar dados privados do tutor.
- Consulta pública: `/public/internacoes/qr/:token`, sem login.

## Navegação clínica

- A internação tem abas **Resumo**, **Evoluções** e **Cuidados**, com identificação e alertas acima das abas. A recepção vê apenas o resumo. No acesso do auxiliar, ficam disponíveis Cuidados e Evoluções, sem dados privados do tutor.
- As abas aceitam setas esquerda/direita e Home/End pelo teclado. A seleção fica na URL (`aba`) e os painéis permanecem montados para preservar o estado durante a troca.
- O diretório de Cuidados usa cartões com foto, nome, baia e código, busca sem distinção de acentos e paginação. A consulta pelo código continua disponível para internações encerradas.
- O checklist e as confirmações de alimentação exibem a foto e a identificação do paciente. As imagens usam os endpoints autenticados já existentes; a ausência de foto mantém a identificação textual.
- Buscas, filtros e páginas dos cadastros, internações e cuidados ficam na URL (`busca`, `situacao`, `pagina`). Alimentação também preserva `dia` e `filtro`. Os links de retorno às listas preservam esse endereço.
- A posição de rolagem é guardada em `sessionStorage`, por conta/perfil e endereço da lista. A restauração aguarda o carregamento dos dados e funciona tanto pelos links de retorno quanto pelo botão Voltar do navegador. Não depende de novos endpoints. Sem armazenamento disponível, a navegação e os filtros continuam funcionando.

Com o Vite em execução, `npm run test:ux` verifica fotos nas refeições e confirmações, abas pelo teclado, busca dos cartões, retorno com filtros/paginação/rolagem, recuperação após falha e layouts de celular/tablet/computador. Essa verificação também integra `npm run test:e2e`.

## Permissões e integração

Administradores, veterinários e recepcionistas acessam cadastros e internações. Alimentação e jejum são disponíveis para administradores, veterinários e auxiliares técnicos. A API valida as mesmas permissões das telas e bloqueia alimentação durante jejum ou depois do encerramento.

As fotos ficam no disco do backend, e não no armazenamento do navegador. O frontend carrega as imagens usando a sessão autenticada e libera os URLs temporários ao trocar de paciente/tela. Consulte o README da raiz para configurar o diretório e os volumes.

Os horários são apresentados no fuso do dispositivo e enviados à API como instantes UTC. O checklist é atualizado a cada minuto e ao voltar à janela. A conclusão cria o registro clínico no servidor.

Configure `QRVET_FRONTEND_URL` no backend com a origem do frontend acessível pelos celulares da equipe e pelo tutor (porta `3000` no Docker principal e `5173` em desenvolvimento). Os links herdam essa origem; `QRVET_PUBLIC_URL` permite sobrescrevê-la. O QR Code abre `/internacoes/:id` com login; auxiliares são direcionados aos cuidados daquela internação. O botão “Enviar link por e-mail” usa o SMTP do backend para enviar `/public/internacoes/qr/:token` ao e-mail cadastrado do tutor. A impressão contém apenas a etiqueta do animal e seu QR Code.

## Guia de primeiros passos

No primeiro acesso autenticado, um convite permite conhecer o sistema sem interromper a tarefa atual. O guia adapta as etapas ao perfil (administrador, veterinário, recepcionista, auxiliar ou tutor). O botão **Ajuda** no cabeçalho permite retomar ou rever o guia. Fechar ou escolher “Continuar depois” guarda a etapa; concluir permite recomeçar na próxima abertura.

O progresso é salvo apenas neste navegador, por usuário e perfil, na chave `qrvet:onboarding:v1:<id>:<perfil>`. Outro navegador ou a limpeza do armazenamento local faz o convite aparecer novamente. Sem armazenamento disponível, o guia continua funcionando durante a sessão. Não há alteração no backend.

As dicas “Como usar esta tela” abrem ao passar o mouse, focar pelo teclado ou tocar no ícone. Escape, saída do foco ou toque fora fecham a dica. O guia usa diálogo com foco contido e retorna o foco ao botão de origem ao fechar.

Com o Vite em execução, `npm run test:onboarding` verifica os cinco perfis, retomada, conclusão, reabertura, teclado, dicas e layout móvel usando respostas HTTP simuladas. Para usar o Edge: `$env:PLAYWRIGHT_CHANNEL='msedge'`.

## Executar verificações

```powershell
npm run build
npm run lint
# Em outro terminal, mantenha npm run dev em execução.
$env:PLAYWRIGHT_CHANNEL='msedge'
npm run test:e2e
```

Sem Edge, instale o Chromium com `npx playwright install chromium` e omita `PLAYWRIGHT_CHANNEL`. A suíte inclui `clinic-smoke.mjs` (fluxos anteriores e permissões), `mvp-smoke.mjs` (cadastro conjunto, fotos, edição, agenda e checklist), `onboarding-smoke.mjs`, `dashboard-smoke.mjs` e `clinical-records-smoke.mjs` (perfil editável, alertas, evoluções e celular). Capturas regeneráveis ficam em `tests/artifacts/` e `test-results/`.

Os testes de navegador usam respostas simuladas da API. A integração dos serviços com banco e autorização é verificada pelos testes Java; banco MySQL, volumes Docker e entrega de e-mails precisam de verificação no ambiente de execução.
