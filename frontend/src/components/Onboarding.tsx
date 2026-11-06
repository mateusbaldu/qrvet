import { useEffect, useRef, useState } from 'react'
import { ArrowLeft, ArrowRight, BookOpen, Check, Compass } from 'lucide-react'
import type { Role, User } from '../services/api'
import { Modal } from './ClinicUI'
import '../styles/onboarding.css'

type Step = { title: string; text: string; items: string[]; tip: string }
const registration: Step = {
  title: 'Comece pelo paciente e pelo tutor',
  text: 'Em Pacientes, busque um cadastro existente antes de criar outro.',
  items: ['Use “Cadastrar paciente” para registrar o animal e escolher um tutor existente ou cadastrar um novo.', 'Na ficha, confira os dados, adicione uma foto e consulte as internações anteriores.', 'Se o paciente precisar ficar na clínica, marque “Abrir internação após salvar” no cadastro.'],
  tip: 'Tutor é a pessoa responsável pelo animal. Um tutor pode ter mais de um paciente.',
}
const admission: Step = {
  title: 'Da entrada ao encerramento',
  text: 'Em Internações, acompanhe quem está na clínica e consulte os atendimentos encerrados.',
  items: ['Para abrir uma internação, selecione o paciente, uma baia disponível e o veterinário responsável.', 'Abra “Acompanhar internação” para consultar os detalhes e acessar o QR Code.', 'Ao encerrar, confira o desfecho: a baia será liberada, o jejum encerrado e as refeições pendentes canceladas.'],
  tip: 'Se não conseguir iniciar, confira se há baia disponível, paciente sem internação ativa e veterinário com convite aceito.',
}
const care: Step = {
  title: 'Organize os cuidados do dia',
  text: 'Alimentação reúne as refeições da clínica. Cuidados permite consultar e registrar os cuidados por paciente.',
  items: ['Agende o alimento, a quantidade, os dias e os horários conforme a orientação da equipe clínica.', 'Use os filtros do checklist para encontrar refeições atrasadas, pendentes ou concluídas.', 'Confirme a refeição somente depois de oferecê-la e registre a aceitação. O sistema guarda o horário e o responsável.'],
  tip: 'O jejum ativo bloqueia a conclusão de refeições. Consulte a orientação da equipe antes de alterar o jejum.',
}
const qr: Step = {
  title: 'Dois acessos, cada um com seu propósito',
  text: 'O QR Code da internação é usado pela equipe. O tutor recebe outro link para acompanhar o paciente.',
  items: ['A equipe aponta a câmera do celular para a etiqueta e entra com sua conta para abrir a internação.', 'Em “Consultar QR Code”, também é possível colar o endereço ou digitar o número da internação.', 'Na internação, “Enviar link por e-mail” envia o acompanhamento ao e-mail cadastrado do tutor, sem exigir login.'],
  tip: 'O acesso público mostra um resumo. Os registros detalhados ficam no acesso da equipe, conforme as permissões.',
}
function stepsFor(role: Role): Step[] {
  if (role === 'TUTOR') return [{
    title: 'Acompanhe seu animal', text: 'A clínica envia por e-mail um link de acompanhamento da internação.',
    items: ['Abra o link recebido para consultar a situação e as informações disponíveis, sem precisar entrar no sistema.', 'Use “Atualizar informações” para buscar os dados mais recentes.', 'Para detalhes sobre a evolução do paciente, entre em contato com a equipe da clínica.'],
    tip: 'O QR Code afixado na clínica é destinado à equipe. Para acompanhar seu animal, use o link enviado a você.',
  }]
  if (role === 'AUXILIAR_TECNICO') return [care, {
    ...qr, items: [qr.items[0], qr.items[1], 'Ao abrir o QR Code da internação, você será direcionado aos cuidados daquele paciente.'],
  }]
  const steps = [registration, admission]
  if (role !== 'RECEPCIONISTA') steps.push(care)
  steps.push(qr)
  if (role === 'ADMIN') steps.push({
    title: 'Prepare a clínica para a equipe', text: 'Organize os acessos e os espaços antes dos primeiros atendimentos.',
    items: ['Em Equipe, convide os profissionais e escolha o perfil adequado para cada função.', 'Em Baias, cadastre os espaços e sinalize os que estão em manutenção.', 'Em Sessões, consulte e encerre acessos de um membro quando necessário.'],
    tip: 'Cada perfil vê as ferramentas permitidas para sua função. Um veterinário precisa aceitar o convite para aparecer na abertura de internação.',
  })
  return steps
}

function readProgress(key: string) {
  try {
    const saved = JSON.parse(localStorage.getItem(key) ?? 'null')
    return { seen: saved?.seen === true, step: Number.isInteger(saved?.step) ? saved.step as number : 0 }
  } catch { return { seen: false, step: 0 } }
}

export function Onboarding({ user, open, onOpen, onClose }: {
  user: User; open: boolean; onOpen: () => void; onClose: () => void
}) {
  const key = `qrvet:onboarding:v1:${user.id}:${user.role}`
  const [progress, setProgress] = useState(() => readProgress(key))
  const steps = stepsFor(user.role)
  const index = Math.max(0, Math.min(progress.step, steps.length - 1))
  const current = steps[index]
  const heading = useRef<HTMLHeadingElement>(null)
  useEffect(() => { if (open) heading.current?.focus() }, [open, index])
  function save(step: number, seen = true) {
    const next = { step, seen }
    setProgress(next)
    try { localStorage.setItem(key, JSON.stringify(next)) } catch { /* Help remains usable without storage. */ }
  }
  function close() { save(index); onClose() }
  return <>
    {!progress.seen && !open && <section className="onboarding-welcome" aria-label="Boas-vindas ao QRVet">
      <span className="onboarding-mark"><Compass size={26} aria-hidden="true" /></span>
      <div><strong>Primeira vez por aqui?</strong><p>Conheça o QRVet em poucos passos, com um guia para sua função.</p></div>
      <div className="onboarding-welcome-actions">
        <button type="button" className="onboarding-start" onClick={onOpen}><BookOpen size={17} /> Conhecer o sistema</button>
        <button type="button" onClick={() => save(0)}>Agora não</button>
      </div>
    </section>}
    {open && <Modal title="Seu guia do QRVet" onClose={close}>
      <div className="onboarding-guide">
        <p className="onboarding-progress" aria-live="polite">Passo {index + 1} de {steps.length}</p>
        <div className="onboarding-track" aria-hidden="true">{steps.map((step, n) => <span key={step.title} className={n <= index ? 'complete' : ''} />)}</div>
        <h3 ref={heading} tabIndex={-1} data-autofocus>{current.title}</h3>
        <p>{current.text}</p>
        <ol>{current.items.map((item, n) => <li key={item}><span aria-hidden="true">{n + 1}</span>{item}</li>)}</ol>
        <aside className="onboarding-note"><BookOpen size={18} aria-hidden="true" /><p>{current.tip}</p></aside>
        <p className="onboarding-replay">Você pode rever este guia pelo botão Ajuda, no topo de qualquer tela.</p>
        <div className="onboarding-controls">
          <button type="button" onClick={close}>Continuar depois</button>
          <div>{index > 0 && <button type="button" onClick={() => save(index - 1)}><ArrowLeft size={16} /> Anterior</button>}
            <button type="button" className="clinic-primary" onClick={() => {
              if (index === steps.length - 1) { save(0); onClose() }
              else save(index + 1)
            }}>{index === steps.length - 1 ? <>Concluir guia <Check size={16} /></> : <>Próximo <ArrowRight size={16} /></>}</button>
          </div>
        </div>
      </div>
    </Modal>}
  </>
}
