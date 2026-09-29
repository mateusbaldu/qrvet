# qrvet
Projeto de Trabalho de Graduação da Fatec Ipiranga.

Membros do grupo:
- Mateus Balduino da Silva
- Anthony Akio Neves
- Vitor Vieira da Hora
- Kauan Torres
- Washington Luis

## MVP: rotina da clínica

- **Cadastro em uma tela:** em Pacientes → Cadastrar paciente, selecione um tutor existente (busca por nome/CPF) ou preencha um novo tutor. Tutor e paciente são gravados na mesma transação. A opção “Abrir internação após salvar” leva à internação com o paciente selecionado.
- **Ficha editável:** altere dados do paciente e do tutor pela ficha, consulte internações anteriores e use os links de telefone/e-mail. Também é possível adicionar um paciente a partir do cartão do tutor.
- **Foto do paciente:** envie, substitua ou remova uma foto na ficha; também é possível enviar durante o cadastro. Aceita JPG/PNG até 5 MB e 16 megapixels. O servidor valida o conteúdo e salva uma versão JPEG de até 1280 px. Se o envio falhar após o cadastro, a ficha permite tentar novamente sem duplicar o paciente.
- **Agenda de alimentação:** escolha uma internação ativa, alimento, quantidade e até oito horários por dia, por um período de até 31 dias. Horários que já passaram no dia inicial são ignorados. Os horários usam o fuso local do dispositivo e são enviados à API em UTC.
- **Checklist geral:** o menu Alimentação reúne as refeições por horário, com busca, seleção de dia e filtros de pendentes, atrasadas, jejum, concluídas e canceladas. Pacientes internados sem refeições programadas no dia também são indicados. O painel inicial resume as refeições de hoje para a equipe clínica.
- **Cuidados rastreáveis:** concluir uma refeição exige confirmação e gera um registro com profissional, horário real e observação de aceitação. A API impede concluir antes do horário ou durante jejum; repetir a conclusão não cria outro registro. Cancelamentos exigem motivo, e o horário cancelado pode receber um novo agendamento. Encerrar a internação cancela as refeições pendentes, encerra o jejum e libera a baia.
- **QR Code no vidro da clínica:** a etiqueta identifica o paciente e a internação e abre diretamente `/internacoes/:id`, com login da equipe. O auxiliar é direcionado aos cuidados daquele animal, respeitando suas permissões. Na mesma tela, “Enviar link por e-mail” envia ao e-mail cadastrado do tutor o acompanhamento público, sem QR Code e sem exigir login.

Configure `QRVET_PUBLIC_URL` com o endereço do frontend acessível pelos celulares da equipe e pelo tutor. O envio usa a configuração SMTP existente (`SPRING_MAIL_*` e `QRVET_MAIL_FROM`); falhas de envio são mostradas na tela para permitir nova tentativa. O envio é feito ao clicar no botão.

### Fotos e persistência

Por padrão, os arquivos ficam em `./uploads`, relativo à pasta de execução do backend. É possível alterar o diretório com `QRVET_UPLOAD_DIR`. As fotos só são entregues pela API autenticada aos perfis com acesso ao cadastro do paciente.

Os dois arquivos Compose usam o volume `patient_photos`, montado em `/opt/app/uploads`. Ele preserva as fotos ao recriar os contêineres; o volume do banco preserva os cadastros e horários. Para backup, guarde **o banco e o volume de fotos juntos**. A opção `docker compose down -v` remove os volumes e seus dados.

### Atualizar e demonstrar

Na raiz do projeto, execute `docker compose up -d --build` para usar as alterações locais. O Flyway aplica a migração `V9__patient_photo_and_feeding_schedule.sql` ao iniciar o backend.

Roteiro sugerido para a apresentação do TCC:

1. Cadastre um tutor e um paciente na mesma tela, com foto.
2. Marque a opção de abrir internação e selecione baia e veterinário.
3. Agende refeições para hoje e amanhã; abra o checklist geral.
4. Demonstre o bloqueio por jejum e a conclusão de uma refeição cujo horário já chegou.
5. Confira o profissional e a observação no histórico, consulte o QR Code e encerre a internação.

Administradores, veterinários e recepcionistas gerenciam pacientes/tutores. Administradores, veterinários e auxiliares técnicos usam alimentação e jejum. Os horários são uma agenda consultada na aplicação, com atualização a cada minuto; a agenda não envia notificações externas.

### Validação

- Backend: `cd backend` e `./mvnw test` (Windows: `mvnw.cmd test`). Os testes usam H2 e verificam cadastro conjunto, edição, fotos, permissões, agendamento, duplicidades, jejum, conclusão e encerramento.
- Frontend: `cd frontend`, `npm ci`, `npm run build` e `npm run lint`.
- Navegador: com `npm run dev` em execução, execute `npm run test:e2e`. Instale o Chromium com `npx playwright install chromium`, ou use o Edge instalado com `$env:PLAYWRIGHT_CHANNEL='msedge'` no PowerShell.

Os testes de navegador simulam as respostas HTTP e verificam os fluxos e layouts de desktop/celular; os testes da API usam os serviços reais com banco H2. A aplicação das migrações MySQL e a persistência dos volumes Docker devem ser conferidas no ambiente de execução antes da apresentação.

## Execução com Docker

Os Dockerfiles estão em `backend/Dockerfile` e `frontend/Dockerfile`. O
`docker-compose.yml` contém toda a configuração necessária, inclusive as
variáveis de execução, portanto não é necessário criar um arquivo `.env`.

Para publicar as imagens no Docker Hub, após instalar e iniciar o Docker
Desktop, execute na raiz do projeto:

```powershell
docker login
docker compose build
docker compose push
```

Para executar em outra máquina (por exemplo, a do professor), copie apenas o
arquivo `docker-compose.yml` e execute:

```powershell
docker compose pull
docker compose up -d
```

A aplicação fica disponível em `http://localhost:3000`. Para acompanhar a
inicialização, use `docker compose logs -f`.

> Atenção: o Compose inclui credenciais por solicitação do projeto. Não o
> publique em repositório público nem reutilize essas credenciais em produção.
