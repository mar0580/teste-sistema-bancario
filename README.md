# parcao-back
Projeto Sistema Bancário - teste de desempenho para Cast Group

Motivos pela escolha do tipo de bloqueio:
Alta concorrência: O controle otimista é mais eficiente em cenários onde há muitas leituras e poucas escritas concorrentes, como em sistemas bancários com muitas consultas de saldo e poucas operações de crédito/débito simultâneas.


Evita bloqueios desnecessários: O controle otimista não bloqueia os registros no banco de dados, permitindo maior escalabilidade e desempenho.


Detecção de conflitos: Ele detecta conflitos apenas no momento da gravação, garantindo consistência sem impactar a performance.
