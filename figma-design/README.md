# Protótipo QRVET

[Abrir o protótipo navegável](https://www.figma.com/proto/OD9iJUZvOZx1GhdthN0REM?node-id=9-335&starting-point-node-id=9%3A335) · [Abrir o arquivo editável](https://www.figma.com/design/OD9iJUZvOZx1GhdthN0REM)

A entrega atual tem **96 telas e estados de interface, além da capa**, 809 conexões e cinco pontos de partida: apresentação, clínica no computador, clínica no celular, auxiliar no celular e acompanhamento do tutor. As 97 telas estão acessíveis a partir da capa, sem destinos inválidos.

As telas usam camadas nativas editáveis, componentes, Auto Layout, DM Sans e variáveis de cor e espaçamento, preservando a paleta verde do sistema. Incluem acesso e recuperação de conta, dashboard, pacientes e fotos, tutores, baias, internações, alertas, evoluções, alimentação e agendamentos, medicação, jejum, alta, equipe, sessões, auditoria, edição do perfil, ajuda e acompanhamento público. Há estados de carregamento, erro, lista vazia e confirmação.

## Validação e limites

Foi conferido o alcance real das 97 telas pelas conexões de navegação. A revisão visual incluiu internação no computador, cuidados e confirmação de refeição no celular, acompanhamento público, baias e capa. Foram corrigidos espaçamento, quebra de texto, largura de botões e contraste do menu.

O protótipo usa dados fictícios. Os cliques demonstram jornadas; campos, filtros, envio de e-mail, download e salvamento não executam operações reais. O símbolo de QR nas telas é ilustrativo. No aplicativo, o QR é gerado pela API e a câmera abre o link da internação.

Fotos de demonstração: Unsplash, [Luna](https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba) e [Thor](https://images.unsplash.com/photo-1552053831-71594a27632d).

## Fontes locais

- `prototype-state.json`: inventário de páginas, componentes, telas, destinos e validação.
- `figma-foundations.js`, `figma-components.js` e `figma-builders.js`: fundamentos, componentes e funções de montagem.
- `batch-01.js` a `batch-07.js`: lotes de construção das telas na Plugin API do Figma.
- `extract-icons.mjs`, `icon-sources.json` e `assets/`: fontes dos ícones e fotos.

Os scripts são fontes usadas pela ferramenta Figma e não são executáveis diretamente pelo Node. Não sincronizam alterações automaticamente. O inventário registra as conexões e a validação final.

## Versão anterior

O arquivo antigo `1eK47QSuqdE98UiNIMt5dc`, a pasta `screens/` e o gerador `generate-qrvet-screens.mjs` correspondem à primeira versão de 21 telas em SVG. Foram preservados como histórico e não representam a entrega atual. Use os links no início deste documento.
