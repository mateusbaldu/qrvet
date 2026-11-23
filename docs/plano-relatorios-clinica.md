# Plano de Ação e Contrato da API — Painel de Relatórios Gerenciais da Clínica (CSU010 / RF010)

> [!NOTE]
> O **Backend e os Testes Automatizados** já estão implementados na branch `feat/relatorios-clinica`. Este documento serve como referência e contrato da API para a implementação do **Frontend** (`/relatorios`) pelo restante da equipe.

---

## 1. Status Atual dos Casos de Uso do MVP

Todos os 10 casos de uso especificados na planilha do projeto possuem implementação funcional na codebase:

| Código | Caso de Uso | RF | Status Atual | Principais Arquivos |
| :--- | :--- | :--- | :---: | :--- |
| **CSU001** | Realizar Login | RF001 | Implementado | `AuthController.java`, `AuthService.java`, `Login.tsx`, `ForgotPassword.tsx` |
| **CSU002** | Consultar Internação via QR Code | RF006, RF008 | Implementado | `InternacaoController.java`, `PublicInternacaoController.java`, `Hospitalizations.tsx`, `ClinicAccess.tsx` |
| **CSU003** | Abrir Internação | RF002, RF003, RF007 | Implementado | `InternacaoController.java`, `InternacaoService.java`, `Hospitalizations.tsx` |
| **CSU004** | Cadastrar Paciente | RF002 | Implementado | `PacienteController.java`, `TutorController.java`, `Registry.tsx`, `PatientEditor.tsx` |
| **CSU005** | Registrar Alimentação | RF004, RF006, RF008 | Implementado | `AgendaAlimentacaoController.java`, `NutricaoClinicaService.java`, `Feeding.tsx` |
| **CSU006** | Registrar Medicação | RF005, RF008 | Implementado | `InternacaoController.java`, `MedicacaoService.java`, `Hospitalizations.tsx` |
| **CSU007** | Gerenciar Baias | RF007 | Implementado | `BaiaController.java`, `BaiaService.java`, `Registry.tsx` |
| **CSU008** | Visualizar Histórico do Paciente | RF008 | Implementado | `Registry.tsx`, `Hospitalizations.tsx`, `EvolucaoController.java`, `EvolutionNotes.tsx` |
| **CSU009** | Gerenciar Usuários | RF009 | Implementado | `UserController.java`, `UserService.java`, `Team.tsx`, `ClinicAccess.tsx` |
| **CSU010** | Gerar Relatórios | RF010 | **Backend Concluído** (`RelatorioController`, `RelatorioClinicoService`, `AuditLogController`) | `RelatorioController.java`, `RelatorioClinicoService.java`, `RelatorioClinicoResponse.java` |

---

## 2. Contrato da API Implementada no Backend

Base: `/qrvet/api/v1`. Todos os endpoints exigem autenticação com perfil **`ADMIN`** (`@PreAuthorize("hasRole('ADMIN')")`). Outros perfis recebem `403 Forbidden`.

### 2.1. `GET /v1/relatorios/clinica`

Retorna os indicadores consolidados de performance da clínica.

#### Parâmetros de Query (todos opcionais)
- `startDate` (`Instant` ISO-8601 UTC, ex.: `2026-11-01T00:00:00.000Z`): filtra registros a partir desta data/hora.
- `endDate` (`Instant` ISO-8601 UTC, ex.: `2026-11-30T23:59:59.999Z`): filtra registros até esta data/hora. Se `endDate < startDate`, retorna `400 Bad Request`.
- `veterinarioId` (`Long`): filtra as métricas de internações, cuidados e ranking para um veterinário específico.

#### Exemplo de Resposta (`200 OK` — `RelatorioClinicoResponse`)
```json
{
  "periodo": {
    "startDate": "2026-11-01T00:00:00Z",
    "endDate": "2026-11-30T23:59:59.999Z",
    "veterinarioId": null
  },
  "resumoInternacoes": {
    "totalNoPeriodo": 12,
    "ativas": 3,
    "altas": 8,
    "obitos": 1,
    "encerradasNoPeriodo": 9,
    "tempoMedioInternacaoHoras": 36.5
  },
  "resumoCuidados": {
    "evolucoesRegistradas": 28,
    "medicacoesAplicadas": 45,
    "alimentacoesRegistradas": 60,
    "refeicoesAgendadas": 70,
    "refeicoesConcluidas": 60,
    "refeicoesPendentes": 6,
    "refeicoesCanceladas": 4,
    "jejunsIniciados": 5
  },
  "atendimentosPorVeterinario": [
    {
      "veterinarioId": 2,
      "veterinarioNome": "Dra. Helena Costa",
      "veterinarioEmail": "helena@qrvet.test",
      "totalInternacoes": 8,
      "ativas": 2,
      "altas": 5,
      "obitos": 1,
      "evolucoesRegistradas": 19
    },
    {
      "veterinarioId": 3,
      "veterinarioNome": "Dr. Rafael Lima",
      "veterinarioEmail": "rafael@qrvet.test",
      "totalInternacoes": 4,
      "ativas": 1,
      "altas": 3,
      "obitos": 0,
      "evolucoesRegistradas": 9
    }
  ],
  "produtividadeEquipe": [
    {
      "usuarioId": 5,
      "nome": "Joana Auxiliar",
      "email": "joana@qrvet.test",
      "role": "AUXILIAR_TECNICO",
      "evolucoesRegistradas": 4,
      "medicacoesAplicadas": 35,
      "alimentacoesRegistradas": 50,
      "totalAcoesClinicas": 89
    }
  ],
  "utilizacaoBaias": [
    {
      "baiaId": 1,
      "identificacao": "B-01",
      "statusAtual": "OCUPADA",
      "totalInternacoesNoPeriodo": 5
    }
  ]
}
```

### 2.2. `GET /v1/relatorios/clinica/export`

Aceita os mesmos parâmetros de query (`startDate`, `endDate`, `veterinarioId`) e retorna o arquivo **`relatorio-performance-clinica.csv`** (`text/csv; charset=UTF-8`, com BOM UTF-8 `\uFEFF` para abrir direto no Excel com acentuação correta), contendo as seções:
1. `PERÍODO DO RELATÓRIO`
2. `RESUMO DE INTERNAÇÕES`
3. `RESUMO DE CUIDADOS CLÍNICOS`
4. `ATENDIMENTOS POR VETERINÁRIO`
5. `PRODUTIVIDADE DA EQUIPE`
6. `UTILIZAÇÃO DE BAIAS`

### 2.3. Enriquecimento da Auditoria (`HOSPITALIZATION_CREATED`)
Em `InternacaoService.open(...)`, o evento de auditoria `HOSPITALIZATION_CREATED` agora também registra `"veterinarioId"` e `"veterinarioNome"` no JSON `details`.

---

## 3. Guia para Implementação do Frontend (`frontend/`)

1. **Rota e Menu Lateral (Somente Admin):**
   - `frontend/src/routes.ts`: adicionar `{ path: '/relatorios', label: 'Relatórios', roles: ['ADMIN'] }` em `appRoutes`.
   - `frontend/src/components/AppLayout.tsx`:
     - Adicionar ícone (ex.: `BarChart3` do `lucide-react`) em `navIcons['/relatorios']`.
     - Incluir `'/relatorios'` no grupo `'Administração'` (`['/equipe', '/relatorios', '/sessoes', '/auditoria']`).
   - `frontend/src/App.tsx`:
     - Dentro de `<Route element={<AdminRoute />}>`, registrar `<Route path="/relatorios" element={<Reports />} />`.
2. **Página `frontend/src/pages/Reports.tsx`:**
   - **Filtros:** `Data inicial`, `Data final` e seletor opcional de `Veterinário` (`/internacoes/veterinarios`), convertendo datas para ISO (`toStartOfDayIso` / `toEndOfDayIso` como em `AuditLogs` de `ClinicAccess.tsx`).
   - **Botão de exportação:** `Baixar relatório (CSV)` consumindo `/relatorios/clinica/export` com `responseType: 'blob'`.
   - **Cards de resumo (`clinic-stats`):** Total no período, Ativas, Altas, Óbitos, Tempo médio (h), Refeições concluídas e Medicações aplicadas.
   - **Tabelas (`clinic-table`):**
     1. *Atendimentos por Veterinário* (`atendimentosPorVeterinario`)
     2. *Produtividade da Equipe nos Cuidados* (`produtividadeEquipe`)
     3. *Utilização e Giro de Baias* (`utilizacaoBaias`)
