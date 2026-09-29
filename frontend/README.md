# Frontend QRVet

Execute `npm ci` e `npm run dev` nesta pasta. O Vite atende em http://localhost:5173 e encaminha `/qrvet/api` para o backend em http://localhost:8080. Para outra origem da API, configure `VITE_API_URL` antes da compilação.

## Fluxos disponíveis

- Autenticação, convite, recuperação de senha e perfil.
- Equipe e gestão de sessões (administrador).
- Painel inicial com ocupação das baias, internações e resumo das refeições de hoje.
- Cadastro conjunto de paciente e tutor, busca de tutor existente, edição de ambos e atalho para abrir internação.
- Fotos locais: prévia, upload autenticado, troca e remoção. Falhas de upload preservam o cadastro salvo.
- Baias: cadastro, edição e manutenção; ocupação e liberação automáticas durante a internação.
- Internações: abertura com seleção de paciente/baia/veterinário, filtros, histórico, encerramento, QR Code para o vidro da clínica e envio do link de acompanhamento ao tutor por e-mail.
- Alimentação: agenda por data e horários, repetição diária limitada a 31 dias, checklist geral com filtros, conclusão com observação, cancelamento e indicação de pacientes sem horários.
- Cuidados: agenda individual, histórico de alimentação e início/fim de jejum. O auxiliar escolhe um paciente no diretório de cuidados, sem acessar dados privados do tutor.
- Consulta pública: `/public/internacoes/qr/:token`, sem login.

## Permissões e integração

Administradores, veterinários e recepcionistas acessam cadastros e internações. Alimentação e jejum são disponíveis para administradores, veterinários e auxiliares técnicos. A API valida as mesmas permissões das telas e bloqueia alimentação durante jejum ou depois do encerramento.

As fotos ficam no disco do backend, e não no armazenamento do navegador. O frontend carrega as imagens usando a sessão autenticada e libera os URLs temporários ao trocar de paciente/tela. Consulte o README da raiz para configurar o diretório e os volumes.

Os horários são apresentados no fuso do dispositivo e enviados à API como instantes UTC. O checklist é atualizado a cada minuto e ao voltar à janela. A conclusão cria o registro clínico no servidor.

Configure `QRVET_PUBLIC_URL` no backend com a origem do frontend acessível pelos celulares da equipe e pelo tutor. O QR Code abre `/internacoes/:id` com login; auxiliares são direcionados aos cuidados daquela internação. O botão “Enviar link por e-mail” usa o SMTP do backend para enviar `/public/internacoes/qr/:token` ao e-mail cadastrado do tutor. A impressão contém apenas a etiqueta do animal e seu QR Code.

## Verificação

```powershell
npm run build
npm run lint
# Em outro terminal, mantenha npm run dev em execução.
$env:PLAYWRIGHT_CHANNEL='msedge'
npm run test:e2e
```

Sem Edge, instale o Chromium com `npx playwright install chromium` e omita `PLAYWRIGHT_CHANNEL`. A suíte inclui `clinic-smoke.mjs` (fluxos anteriores e permissões) e `mvp-smoke.mjs` (cadastro conjunto, fotos, edição, agenda e checklist). Capturas ficam em `tests/artifacts/`.

Os testes de navegador usam respostas simuladas da API. A integração dos serviços com banco e autorização é verificada pelos testes Java; banco MySQL, volumes Docker e entrega de e-mails precisam de verificação no ambiente de execução.
