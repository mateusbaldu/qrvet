# Frontend QRVet

Execute `npm ci` e `npm run dev` nesta pasta. O Vite atende em http://localhost:5173 e encaminha `/qrvet/api` para o backend em http://localhost:8080. Para outra origem da API, configure `VITE_API_URL` antes da compilação.

## Fluxos disponíveis

- Autenticação, convite, recuperação de senha e perfil.
- Equipe e gestão de sessões (administrador).
- Tutores: cadastro, busca por nome e pacientes vinculados.
- Pacientes: cadastro com tutor, listagem e abertura de internação.
- Baias: listagem por situação; criação, edição e manutenção pelo administrador. Ocupação e liberação são feitas pelo backend durante a internação.
- Internações: abertura, listagem das ativas, consulta por código, detalhes, alta/óbito e QR Code para baixar.
- Cuidados: registro e histórico de alimentação, início/fim e histórico de jejum. Alimentação fica bloqueada enquanto o jejum estiver ativo.
- Consulta pública: `/public/internacoes/qr/:token`, sem login; também é possível colar o código/endereço em `/consultar-qr`.
- Configuração inicial: `/configuracao-inicial`, com a chave de bootstrap exigida pelo backend.

## Permissões e integração

As permissões das telas seguem os serviços Java. Administradores, veterinários e recepcionistas acessam cadastros e internações. Alimentação e jejum são disponíveis para administradores, veterinários e auxiliares técnicos.

A API atual não permite que auxiliares listem internações ou consultem seus detalhes. Por isso, `/cuidados` recebe o código da internação informado pela equipe. O backend rejeita registros em internações encerradas.

Somente administradores podem listar usuários. No formulário de internação, o administrador seleciona o veterinário; os outros perfis informam seu código (preenchido com o próprio código para veterinários).

Configure `QRVET_PUBLIC_URL` no backend com a origem pública do frontend para que o QR Code gerado abra o site correto. Recursos como medicação, edição de pacientes e histórico geral de internações não têm endpoints nesta versão do backend e não foram adicionados como ações fictícias.

## Verificação

- `npm run build`
- `npm run lint`
- Com Vite em execução, rode `node tests/clinic-smoke.mjs`. O teste requer Playwright e Microsoft Edge; `PLAYWRIGHT_MODULE` pode apontar para uma instalação externa do Playwright.

O teste de navegador usa respostas simuladas da API e verifica cadastro, abertura e encerramento de internação, jejum/alimentação, situação de baia, revogação de sessão, permissões e layout público móvel. Não valida banco de dados, entrega de e-mails ou integração real com o backend.
