import React, { useState } from 'react'
import { useBrecho } from '../context/BrechoContext'

export const Login: React.FC = () => {
  const { login } = useBrecho()
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [visivel, setVisivel] = useState(false)
  const [erro, setErro] = useState('')
  return <div className="login-wrap">
    <div className="login-story">
      <div className="brand-header"><div className="brand-mark">ANG</div><div><div className="brand">O Brechó</div><span className="note">Associação Nova Geração</span></div></div>
      <h1>Cuidar das peças.<br/>Apoiar pessoas.</h1>
      <p>Um lugar para organizar as doações, atender os clientes e acompanhar as vendas do nosso brechó.</p>
      <p className="note">Uso interno da equipe da ANG.</p>
    </div>
    <div className="login-card"><h2>Entrar no brechó</h2><p className="subtitle">Use o usuário e a senha da sua equipe.</p>
      <form onSubmit={e => {e.preventDefault(); setErro(''); const res=login(usuario,senha); if(!res.success) setErro(res.error || 'Confira o usuário e a senha.')}}>
        <div className="form-group"><label htmlFor="login-usuario">Usuário</label><input id="login-usuario" autoComplete="username" autoCapitalize="none" value={usuario} onChange={e=>setUsuario(e.target.value)} required /></div>
        <div className="form-group"><label htmlFor="login-senha">Senha</label><input id="login-senha" type={visivel?'text':'password'} autoComplete="current-password" value={senha} onChange={e=>setSenha(e.target.value)} required /><button type="button" className="text-button" aria-pressed={visivel} onClick={()=>setVisivel(!visivel)}>{visivel?'Ocultar senha':'Mostrar senha'}</button></div>
        {erro && <div className="error" role="alert">{erro}</div>}
        <button className="btn success" style={{width:'100%',marginTop:16}}>Entrar</button>
      </form>
      <details style={{marginTop:24,borderTop:'1px solid var(--line)',paddingTop:20}}><summary style={{cursor:'pointer',minHeight:44}}>Experimentar com dados de exemplo</summary><p className="note">Demonstração: os dados ficam somente neste navegador.</p><div className="actions">{[['admin','Gestor'],['atendente','Atendente'],['clara','Triagem']].map(([u,n])=><button key={u} className="btn secondary small" onClick={()=>{setUsuario(u);setSenha('123456');setErro('')}}>{n}</button>)}</div><p className="note">Escolha um perfil e clique em Entrar.</p></details>
    </div>
  </div>
}
