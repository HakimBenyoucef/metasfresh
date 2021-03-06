package de.metas.contracts.refund;

import de.metas.util.Check;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * de.metas.contracts
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Value
public class AssignCandidatesRequest
{
	RefundInvoiceCandidate refundInvoiceCandidate;

	AssignableInvoiceCandidate assignableInvoiceCandidate;

	RefundConfig refundConfig;

	@Builder(toBuilder = true)
	private AssignCandidatesRequest(
			@NonNull final RefundInvoiceCandidate refundInvoiceCandidate,
			@NonNull final AssignableInvoiceCandidate assignableInvoiceCandidate,
			@NonNull final RefundConfig refundConfig)
	{
		this.refundInvoiceCandidate = refundInvoiceCandidate;

		Check.assumeNotNull(assignableInvoiceCandidate.getRepoId(),
				"This unassignedPair's assignableInvoiceCandidate needs to have a non-null RepoId; this={}", this);
		this.assignableInvoiceCandidate = assignableInvoiceCandidate;

		Check.assumeNotNull(refundConfig.getId(),
				"This unassignedPair's refundConfig needs to have a non-null Id; unassignedPair={}", this);
		this.refundConfig = refundConfig;
	}
}
