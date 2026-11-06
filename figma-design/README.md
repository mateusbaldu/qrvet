# Protótipo QRVet

Arquivo de alta fidelidade: https://www.figma.com/design/1eK47QSuqdE98UiNIMt5dc

O arquivo reúne 21 telas em uma página, com 977 camadas de texto editáveis e 144 conexões de protótipo. Há dois pontos de partida: **Jornada clínica** (login) e **Acompanhamento do tutor** (celular). As conexões cobrem navegação lateral, cadastro, internação, consulta por QR, alimentação e jejum.

Os SVGs em `screens/` são as fontes locais das telas. Para regenerá-los, execute na raiz do repositório:

```powershell
node figma-design/generate-qrvet-screens.mjs
```

As telas usam dados fictícios para demonstração. O gerador atualiza os arquivos SVG locais; alterações posteriores nesses arquivos não são sincronizadas automaticamente com o Figma.
