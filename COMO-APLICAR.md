# Como aplicar na sua branch

Os caminhos abaixo são relativos à raiz do repositório (a pasta do `pom.xml`).

## Passo 1: o commit do teste que falha (com o código ORIGINAL)

Ainda sem mexer em mais nada:

```bash
git checkout main && git pull
git checkout -b corrige-comparacao-de-codigo
```

Copie só `src/test/java/br/ufpb/dcx/poo/biblioteca/AcervoRegressaoTest.java` para o repositório e rode:

```bash
mvn -B test
```

Precisa aparecer **2 testes falhando** nesse arquivo (vermelho). Só então:

```bash
git add src/test/java/br/ufpb/dcx/poo/biblioteca/AcervoRegressaoTest.java
git commit -m "Adiciona teste de regressão para código comparado por referência"
```

## Passo 2: o commit da correção e do restante

1. Copie `src/main/java/.../Fabrica.java`, `dominio/` e `servico/` para o repositório.
2. **Apague** a pasta `src/main/java/br/ufpb/dcx/poo/biblioteca/inicial/`.
3. Substitua `AcervoTest.java` e copie os outros arquivos de teste.
4. Substitua `docs/modelo.puml`.
5. `mvn -B verify` deve terminar em `BUILD SUCCESS`, sem testes pulados.

Sugestão de commits (um por assunto, não um gigante):

```bash
git add src/main/java && git commit -m "Troca == por equals ao localizar item, usando Map por código"
git commit ... "Cria classes de domínio Item, Exemplar e Usuario com invariantes no construtor"
git commit ... "Implementa exemplares com tombo único no acervo inteiro"
git commit ... "Implementa busca por título ignorando maiúsculas"
git commit ... "Reativa testes desabilitados e adiciona testes complementares"
git commit ... "Atualiza diagrama do domínio"
```

Depois `git push -u origin corrige-comparacao-de-codigo`, abra o pull request e peça
revisão a **outro integrante** antes do merge.

## O que sobra para a equipe

- Escolher o acervo e a regra própria (seção "Nossa extensão" do README).
- Colar as seções de `README-secoes.md` no `README.md` e preencher a tabela da equipe.
- Gerar `docs/modelo.png` com o plugin PlantUML e versionar ao lado do `.puml`.
- Preencher `DECLARACAO-DE-USO-DE-IA.md` com o que realmente foi usado.
- Cada integrante precisa ter commits próprios; leiam o código para conseguir explicá-lo.
