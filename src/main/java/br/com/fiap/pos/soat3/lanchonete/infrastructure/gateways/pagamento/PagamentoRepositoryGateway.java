package br.com.fiap.pos.soat3.lanchonete.infrastructure.gateways.pagamento;

import br.com.fiap.pos.soat3.lanchonete.application.gateways.PagamentoGateway;
import br.com.fiap.pos.soat3.lanchonete.application.gateways.RealizaPagamentoMockGateway;
import br.com.fiap.pos.soat3.lanchonete.domain.entity.ItemPedido;
import br.com.fiap.pos.soat3.lanchonete.domain.entity.Pagamento;
import br.com.fiap.pos.soat3.lanchonete.domain.entity.Pedido;
import br.com.fiap.pos.soat3.lanchonete.domain.entity.Produto;
import br.com.fiap.pos.soat3.lanchonete.domain.entity.StatusPedido;
import br.com.fiap.pos.soat3.lanchonete.infrastructure.gateways.pedido.PedidoRepositoryGateway;
import br.com.fiap.pos.soat3.lanchonete.infrastructure.gateways.produto.ProdutoRepositoryGateway;
import br.com.fiap.pos.soat3.lanchonete.infrastructure.integration.MVPResponse;
import br.com.fiap.pos.soat3.lanchonete.infrastructure.persistence.pagamento.PagamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

public class PagamentoRepositoryGateway implements PagamentoGateway {

    private final Logger log = LoggerFactory.getLogger(PagamentoRepositoryGateway.class);
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepositoryGateway pedidoRepositoryGateway;
    private final ProdutoRepositoryGateway produtoRepositoryGateway;
    private final PagamentoEntityMapper pagamentoEntityMapper;
    private final RealizaPagamentoMockGateway realizaPagamentoMockGateway;

    public PagamentoRepositoryGateway(final PagamentoRepository pagamentoRepository,
                                      final PedidoRepositoryGateway pedidoRepositoryGateway,
                                      final ProdutoRepositoryGateway produtoRepositoryGateway,
                                      final PagamentoEntityMapper pagamentoEntityMapper,
                                      final RealizaPagamentoMockGateway realizaPagamentoMockGateway) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepositoryGateway = pedidoRepositoryGateway;
        this.produtoRepositoryGateway = produtoRepositoryGateway;
        this.pagamentoEntityMapper = pagamentoEntityMapper;
        this.realizaPagamentoMockGateway = realizaPagamentoMockGateway;

    }

    @Override
    public Pagamento realizaPagamento(Pagamento pagamento) {

        Pedido pedido = pagamento.getPedido();
        pedido.setStatus(StatusPedido.GERADO);
        pedido.setTotalPedido(getTotal(pedido.getItensPedido()));

        pagamento.setPedido(pedidoRepositoryGateway.cadastraPedido(pedido));

//        MVPResponse response = new MVPResponse(String.valueOf(Math.abs(new Random(pedido.getId()).nextInt())),
//                "http://webhook-mock:9999/mock/".concat(String.valueOf(pagamentoId)));
//        pagamento.setQrCode(response.getCode());
//        pagamento.setWebhook(response.getUrl());

        var pagamentoEntity = pagamentoEntityMapper.toEntity(pagamento);
        pagamento.setId(pagamentoRepository.save(pagamentoEntity).getId());

        pagamento.getPedido().setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        log.info(String.format("Lanchonete: Pagamento do pedido  %s gerado", pagamento.getPedido().getId()));
        return pagamento;
    }

    private String getTotal(List<ItemPedido> itensPedido) {
        BigDecimal total = BigDecimal.ZERO;

        for (ItemPedido itemPedido : itensPedido) {
            Produto produto = produtoRepositoryGateway.buscaProduto(itemPedido.getProduto().getId());
            String valorUnitario = String.valueOf(produto.getValor());
            BigDecimal valor = new BigDecimal(valorUnitario.replaceAll("\\.", "").replace(",", "."));

            BigDecimal multiplicador = new BigDecimal(itemPedido.getQuantidade());

            total = total.add(valor.multiply(multiplicador));
        }
        return String.valueOf(total);
    }
}
