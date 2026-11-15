package br.ufpb.dcx.poo.biblioteca.extensao;

import java.math.BigDecimal;

/**
 * A multa de um empréstimo: quantos dias de atraso e quanto isso custa.
 *
 * @param idDoEmprestimo id do empréstimo que gerou a multa
 * @param tombo tombo do exemplar emprestado
 * @param diasDeAtraso dias entre a data prevista e a devolução (ou a data da consulta)
 * @param valor valor em reais, com duas casas decimais
 */
public record Multa(String idDoEmprestimo, String tombo, long diasDeAtraso, BigDecimal valor) { }