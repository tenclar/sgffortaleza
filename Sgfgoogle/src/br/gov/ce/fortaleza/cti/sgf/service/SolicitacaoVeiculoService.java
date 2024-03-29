/**
 * 
 */
package br.gov.ce.fortaleza.cti.sgf.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.gov.ce.fortaleza.cti.sgf.entity.Falta;
import br.gov.ce.fortaleza.cti.sgf.entity.SolicitacaoVeiculo;
import br.gov.ce.fortaleza.cti.sgf.entity.UG;
import br.gov.ce.fortaleza.cti.sgf.entity.User;
import br.gov.ce.fortaleza.cti.sgf.entity.Veiculo;
import br.gov.ce.fortaleza.cti.sgf.util.SgfUtil;
import br.gov.ce.fortaleza.cti.sgf.util.StatusSolicitacaoVeiculo;

/**
 * @author Deivid
 * @since 22/12/2009
 */
@Repository
@Transactional
public class SolicitacaoVeiculoService extends BaseService<Integer, SolicitacaoVeiculo> {

	private static final Log logger = LogFactory.getLog(BaseService.class);

	@Autowired
	private VeiculoService veiculoService;

	@SuppressWarnings("unchecked")
	public List<SolicitacaoVeiculo> findByUGAndStatus(UG ug, StatusSolicitacaoVeiculo status) {

		List<SolicitacaoVeiculo> solicitacaoVeiculos = new ArrayList<SolicitacaoVeiculo>();
		try {
			Query query = null;

			query = entityManager.createQuery("select s from SolicitacaoVeiculo s where s.uaSolicitante.ug.id = ? and s.status = ? order by s.dataHoraSaida desc");
			query.setParameter(1, ug.getId());
			query.setParameter(2, status);
			solicitacaoVeiculos = query.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return solicitacaoVeiculos;
	}

	@SuppressWarnings("unchecked")
	public List<SolicitacaoVeiculo> findSolVeiculoOrgao(Date dtInicial, Date dtFinal, UG orgao, StatusSolicitacaoVeiculo status) {

		String sql = "select o from SolicitacaoVeiculo o where o.veiculo != null AND o.dataHoraSaida >= :inicio and o.dataHoraRetorno <= :fim";
		StringBuffer hql = new StringBuffer(sql);
		if(orgao != null){
			hql.append(" and o.veiculo.ua.ug.id = :ugid");
		}
		hql.append(" and o.status = :status order by o.dataHoraSaida desc");

		Query query = entityManager.createQuery(hql.toString());
		query.setParameter("inicio", dtInicial);
		query.setParameter("fim", dtFinal);
		query.setParameter("status", status);
		if(orgao != null){
			query.setParameter("ugid", orgao.getId());
		}
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<SolicitacaoVeiculo> findSolicitacoesVeiculos(String placa, StatusSolicitacaoVeiculo status) {
		placa = placa.trim();
		String sql = "select o from SolicitacaoVeiculo o where o.veiculo != null AND o.veiculo.placa = :placa and o.status = :status order by o.dataHoraSaida desc";
		StringBuffer hql = new StringBuffer(sql);
		Query query = entityManager.createQuery(hql.toString());
		query.setParameter("placa", placa.toUpperCase());
		query.setParameter("status", status);
		return query.getResultList();
	}

	/**
	 * 
	 * @param retorna um mapeamento dos kilometros rodados por ve�culo
	 * @param begin
	 * @param end
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	public Map<Veiculo, Object[]>  mapKilometragem(UG ug, Date begin, Date end){
		Map<Veiculo, Object[]> result = new HashMap<Veiculo, Object[]>();
		List<Object> kmRodados = new ArrayList<Object>();
		
		try {
			
			List<Veiculo> veiculos = (List<Veiculo>) entityManager.createNamedQuery("Veiculo.findByUG").setParameter(1, ug.getId()).getResultList();
			
			for(Veiculo v : veiculos){
				StringBuilder sqlRodados = new StringBuilder("SELECT MIN(r.kmsaida) as saida, MAX(r.kmretorno) as retorno, SUM(r.kmretorno - r.kmsaida) as total FROM tb_registroveiculos AS r");
				sqlRodados.append(" INNER JOIN tb_solveiculos AS s ON (r.codsolveiculo = s.codsolveiculo)");
				sqlRodados.append(" INNER JOIN tb_cadveiculo AS v ON (s.codveiculo = v.codveiculo)");
				sqlRodados.append(" WHERE v.placa = '"+v.getPlaca()+"'");
				sqlRodados.append(" AND s.datafim BETWEEN '"+begin+"' AND '"+end+"'");
				sqlRodados.append(" AND s.statussol = 4");
				sqlRodados.append(" GROUP BY v.placa");
				
				Query queryRodados = entityManager.createNativeQuery(sqlRodados.toString());
					
						
				kmRodados = queryRodados.getResultList();
				
				if(kmRodados.size() > 0) {
				
					Object[] arraydb = (Object[]) kmRodados.get(0);
					Object[] array;
					
					//for(Object r : arraydb){
					//	BigDecimal r = new BigDecimal((char[]) r);
					//}
					result.put(v, arraydb);
				}
			}
			
			//StringBuilder sql = new StringBuilder("SELECT s.veiculo.id, MAX(s.kmRetorno), MIN(s.kmSaida) FROM SolicitacaoVeiculo s WHERE s.dataHoraRetorno between ? and ?");
			//if(ug != null){
			//	sql.append(" and s.veiculo.ua.ug.id = '"+ ug.getId() + "'");
			//}
			//sql.append(" GROUP BY s.veiculo.id");
			//Query query = entityManager.createQuery(sql.toString());
			//query.setParameter(1, begin);
			//query.setParameter(2, end);
			//veiculos = query.getResultList();
			
			//for (int i = 0; i < veiculos.size(); i++) {
				//Object[] array = (Object[]) veiculos.get(i);
				//Integer id = (Integer) array[0];
				//Long kmMax = (Long) array[1];
				//Long kmMin = (Long) array[2];


			//	Veiculo veiculo = null;
			//	veiculo = (Veiculo) entityManager.createNamedQuery("Veiculo.findById")
			//						.setParameter(1, id)
			//						.getSingleResult();
			
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	public List<SolicitacaoVeiculo> kilometrosIndividuais(Veiculo veiculo, Date begin, Date end) {
		List<SolicitacaoVeiculo> result = new ArrayList<SolicitacaoVeiculo>();
		
		StringBuilder sql = new StringBuilder("SELECT s FROM SolicitacaoVeiculo s WHERE s.dtRetorno between ? and ?");
		sql.append(" and s.veiculo.placa = '"+ veiculo.getPlaca() + "'");
		sql.append(" and s.status = 4");
		sql.append(" ORDER BY s.dtRetorno");
		
		Query query = entityManager.createQuery(sql.toString());
		query.setParameter(1, begin);
		query.setParameter(2, end);
		result = query.getResultList();
		
		return result;
	}
	/**
	 * s.veiculo.status = 4 � o caso em o ve�culo est� indispon�vel
	 * @param user
	 * @param status
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<SolicitacaoVeiculo> findByUserAndStatus(User user, StatusSolicitacaoVeiculo status) {
		List<SolicitacaoVeiculo> solicitacaoVeiculos = new ArrayList<SolicitacaoVeiculo>();
		try {
			StringBuffer sql = new StringBuffer("select s from SolicitacaoVeiculo s where s.solicitante.codPessoaUsuario = ?");
			if (status != null) {
				sql.append(" and s.status = ?");
			}
			sql.append(" order by s.dataHoraSaida");
			Query query = entityManager.createQuery(sql.toString());
			query.setParameter(1, user.getCodPessoaUsuario());
			if (status != null) {
				query.setParameter(2, status);
			}
			return query.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return solicitacaoVeiculos;
	}

	/**
	 * Encontra os ve�culos dispon�veis em qualquer UG, no caso de adminstrador ou coordenador
	 * Encontra os ve�culos dispon�veis na UG do usu�rio, no caso de chefe de transporte ou chefe se setor
	 * @param solicitacao
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Veiculo> findVeiculosDisponiveis(SolicitacaoVeiculo solicitacao, UG aux) {

		List<Veiculo> veiculos 							= new ArrayList<Veiculo>();
		List<SolicitacaoVeiculo> solicitacaoVeiculos 	= new ArrayList<SolicitacaoVeiculo>();
		List<Falta> faltaVeiculos 						= new ArrayList<Falta>();
		
		StringBuffer hql = new StringBuffer("SELECT s FROM SolicitacaoVeiculo s WHERE ((s.dataHoraRetorno BETWEEN :saida and :retorno) AND " +
		"(s.dataHoraSaida BETWEEN :saida and :retorno)) or (s.dataHoraRetorno BETWEEN :saida AND :retorno) or (s.dataHoraSaida BETWEEN :saida AND :retorno)");
		
		StringBuffer fsql = new StringBuffer("SELECT f.veiculo FROM Falta f WHERE f.dataFalta = :saida");
		
		UG ug = null;
		if(aux != null){
			ug = aux;
		} else {
			if(!SgfUtil.isAdministrador(solicitacao.getSolicitante()) && !SgfUtil.isCoordenador(solicitacao.getSolicitante())){
				ug = solicitacao.getSolicitante().getPessoa().getUa().getUg();
			}
		}
		
		if (ug != null) {
			hql.append("and s.solicitante.pessoa.ua.ug.id = :ugId");
		}
		hql.append(" and s.veiculo.dataDut > now()");
		Query query = entityManager.createQuery(hql.toString());
		query.setParameter("saida", solicitacao.getDataHoraSaida());
		query.setParameter("retorno", solicitacao.getDataHoraRetorno());
		
		Query sql = entityManager.createQuery(fsql.toString());
		sql.setParameter("saida", solicitacao.getDataHoraSaida());
		
		if (ug != null) {
			query.setParameter("ugId", ug.getId());
		}
		
		solicitacaoVeiculos = query.getResultList();
		List<Veiculo> remove = new ArrayList<Veiculo>();
		
		faltaVeiculos = sql.getResultList();
		List<Veiculo> faltosos = new ArrayList<Veiculo>();
		
		for(Veiculo v : faltosos){
			remove.add(v);
		}

		for (SolicitacaoVeiculo sol : solicitacaoVeiculos) {
			remove.add(sol.getVeiculo());
		}

		veiculos = veiculoService.veiculosDisponiveis(ug);
		Collections.sort(veiculos, new Comparator<Veiculo>() {
			public int compare(Veiculo o1, Veiculo o2) {
				return o1.getPlaca().compareTo(o2.getPlaca());
			}
		});
		veiculos.removeAll(remove);
		return veiculos;
	}
	/**
	 * Verifica se o ve�culo possui alguma solicita��o, autoriza��o ou o ve�culo se encontra em rota pra o per�odo informado
	 * @param vid
	 * @param horaSaida
	 * @param horaRetorno // status = 0 => SOLICITADO;  status = 1 => AUTORIZADO; status = 2 => NEGADO;  status = 3 => (EXTERNO OU EM ROTA); status = 4 => FINALIZADO
	 * @return
	 */
	public Boolean isVeiculoDisponivel(Integer vid, Date horaSaida, Date horaRetorno){
		StringBuffer stringQuery = new StringBuffer("SELECT s FROM SolicitacaoVeiculo s WHERE s.veiculo.id = :veiculo and (s.status = 0 or s.status = 1 or s.status = 3) and " +
				"(((s.dataHoraRetorno BETWEEN :saida and :retorno) AND (s.dataHoraSaida BETWEEN :saida and :retorno)) or (s.dataHoraRetorno BETWEEN :saida AND :retorno) or " +
		"(s.dataHoraSaida BETWEEN :saida AND :retorno))");
		Query query = entityManager.createQuery(stringQuery.toString());
		query.setParameter("veiculo", vid);
		query.setParameter("saida", horaSaida);
		query.setParameter("retorno", horaRetorno);
		return query.getResultList().size() > 0;
	}

	@SuppressWarnings("unchecked")
	public List<SolicitacaoVeiculo> pesquisarSolUsuario(User usuario, StatusSolicitacaoVeiculo status) {
		List<SolicitacaoVeiculo> solicitacaoVeiculos = new ArrayList<SolicitacaoVeiculo>();
		Query query = entityManager.createQuery("select s from SolicitacaoVeiculo s where s.solicitante.codPessoaUsuario = ? and s.status = ?");
		query.setParameter(1, usuario.getCodPessoaUsuario());
		query.setParameter(2, status);
		solicitacaoVeiculos = query.getResultList();
		return solicitacaoVeiculos;
	}

	public List<SolicitacaoVeiculo> pesquisarSolicitacaoUser(Integer id, StatusSolicitacaoVeiculo status) {
		return executeResultListQuery("findByUsuarioStatus", id, status);
	}

	/**
	 * retorna a lista de solicitações de veículos
	 * @param veiculo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<SolicitacaoVeiculo> findSolicitacoesVeiculo(Veiculo veiculo, StatusSolicitacaoVeiculo status) {
		List<SolicitacaoVeiculo> solicitacaoVeiculos = null;
		StringBuilder sql = new StringBuilder("select s from SolicitacaoVeiculo s where s.veiculo.id = :id");
		if(status != null){
			sql.append(" and s.status = :status");
		}
		
		sql.append(" ORDER BY s.dtRetorno DESC");

		Query query = entityManager.createQuery(sql.toString());
		query.setParameter("id", veiculo.getId());

		if(status != null){
			query.setParameter("status", status);
		}

		solicitacaoVeiculos = query.getResultList();
		return solicitacaoVeiculos;
	}
}
