# Edição de permissões e programação de alimentação

Base: `/qrvet/api/v1`. As escritas exigem autenticação e CSRF.

## Equipe

`PUT /users/{id}/role` recebe `{ "role": "AUXILIAR_TECNICO" }` e retorna o `UserResponse` atualizado. Aceita os perfis existentes: `ADMIN`, `VETERINARIO`, `RECEPCIONISTA`, `AUXILIAR_TECNICO` e `TUTOR`.

Somente administradores podem alterar permissões. A própria permissão não pode ser alterada (409): outro administrador deve fazer a alteração. As mudanças administrativas são serializadas e o perfil do autor é conferido novamente sob bloqueio. Convites pendentes também podem mudar de perfil, sem ativar a conta. A alteração gera o evento de auditoria `USER_ROLE_UPDATED`, com perfil anterior e novo.

A autorização de cada requisição autenticada usa o perfil atual no banco. Tokens de acesso emitidos antes da alteração deixam de conceder as permissões antigas imediatamente. Contas inativas ou não confirmadas são recusadas. O membro deve recarregar a página para atualizar a navegação exibida.

## Alimentação

`PUT /internacoes/{id}/alimentacao/agenda/{agendaId}` recebe:

```json
{ "alimento": "Dieta pastosa", "quantidade": "50 ml", "horario": "2026-11-16T19:30:00Z" }
```

Retorna `AgendaAlimentacaoResponse`. Administradores, veterinários e auxiliares podem editar uma refeição pendente de internação ativa. A alteração afeta somente essa refeição. Alimento é obrigatório (até 100 caracteres), quantidade é obrigatória (até 50) e horário é obrigatório. Espaços nas extremidades são removidos. Horários usam instantes UTC; a tela converte do horário local do dispositivo.

Reagendar exige horário futuro e não permite colisão com outra refeição não cancelada da mesma internação. É permitido corrigir alimento/quantidade de uma refeição atrasada mantendo exatamente seu horário original. Jejum permite editar a programação e continua bloqueando a conclusão. Refeições concluídas/canceladas retornam 409; internação encerrada retorna 400. IDs de outra internação não permitem editar a refeição (404).

A edição usa o mesmo bloqueio da internação empregado na criação, conclusão, cancelamento e alta. Mantém o ID e não cria registro de alimentação consumida. Ao mudar de dia, a tela abre o dia escolhido no checklist.

## Verificação

`ClinicIntegrationTests` cobre persistência, perfis, tokens antigos, validação, colisões, jejum e histórico. `npm run test:team-feeding-edit` verifica a interface com respostas simuladas, erros/nova tentativa, conversão de horário e layout móvel. Configure `TEST_BASE_URL` para o frontend servido e `PLAYWRIGHT_CHANNEL` para um navegador instalado.
