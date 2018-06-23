package de.metas.handlingunits.reservation;

import static org.adempiere.model.InterfaceWrapperHelper.getContextAware;
import static org.adempiere.model.InterfaceWrapperHelper.loadOutOfTrx;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.apache.commons.lang3.NotImplementedException;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Product;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

import de.metas.document.engine.IDocument;
import de.metas.handlingunits.HuId;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.IHandlingUnitsDAO;
import de.metas.handlingunits.IMutableHUContext;
import de.metas.handlingunits.allocation.transfer.HUTransformService;
import de.metas.handlingunits.allocation.transfer.HUTransformService.HUsToNewCUsRequest;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.X_M_HU;
import de.metas.handlingunits.reservation.HuReservation.HuReservationBuilder;
import de.metas.handlingunits.storage.IHUStorageFactory;
import de.metas.order.OrderLineId;
import de.metas.quantity.Quantity;
import lombok.NonNull;
import lombok.Setter;

/*
 * #%L
 * de.metas.handlingunits.base
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

@Service
public class HuReservationService
{
	/** In unit test mode, we need to use {@link HUTransformService#newInstance(de.metas.handlingunits.IMutableHUContext)} to get a new instance. */
	@Setter
	private Supplier<HUTransformService> huTransformServiceSupplier = () -> HUTransformService.newInstance();

	private final HuReservationRepository huReservationRepository;

	public HuReservationService(@NonNull final HuReservationRepository huReservationRepository)
	{
		this.huReservationRepository = huReservationRepository;
	}

	/**
	 * Creates an HU reservation record and creates dedicated reserved VHUs with HU status "reserved".
	 */
	// Shall we do something if the reservation is less that requested?
	public HuReservation makeReservation(@NonNull final HuReservationRequest reservationRequest)
	{
		final ITrxManager trxManager = Services.get(ITrxManager.class);
		final HuReservation persistedReservation = trxManager
				.call(() -> {
					final HuReservation huReservation = makeReservation0(reservationRequest);
					huReservationRepository.save(huReservation);
					return huReservation;
				});
		return persistedReservation;
	}

	private HuReservation makeReservation0(@NonNull final HuReservationRequest reservationRequest)
	{

		final List<HuId> huIds = Check.assumeNotEmpty(reservationRequest.getHuIds(),
				"the given request needs to have huIds; request={}", reservationRequest);

		final IHandlingUnitsDAO handlingUnitsDAO = Services.get(IHandlingUnitsDAO.class);
		final IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);

		final List<I_M_HU> hus = handlingUnitsDAO.retrieveByIds(huIds);

		final HUsToNewCUsRequest husToNewCUsRequest = HUsToNewCUsRequest.builder()
				.sourceHUs(hus)
				.qtyCU(reservationRequest.getQtyToReserve())
				.onlyFromActiveHUs(true)
				.keepNewCUsUnderSameParent(true)
				// TODO: add productId to the request, for fuck's sake
				.build();

		final List<I_M_HU> newCUs = huTransformServiceSupplier
				.get()
				.husToNewCUs(husToNewCUsRequest);

		final IHUStorageFactory storageFactory = handlingUnitsBL.getStorageFactory();
		final I_M_Product productRecord = loadOutOfTrx(reservationRequest.getProductId(), I_M_Product.class);
		final I_C_UOM uomRecord = reservationRequest.getQtyToReserve().getUOM();

		final HuReservationBuilder reservationBuilder = HuReservation.builder();

		Quantity reservedQtySum = Quantity.zero(uomRecord);
		for (final I_M_HU newCU : newCUs)
		{
			final Quantity qty = storageFactory
					.getStorage(newCU)
					.getQuantity(productRecord, uomRecord);

			reservedQtySum = reservedQtySum.add(qty);
			reservationBuilder.vhuId2reservedQty(HuId.ofRepoId(newCU.getM_HU_ID()), qty);

			final IMutableHUContext huContext = handlingUnitsBL.createMutableHUContext(getContextAware(newCU));
			handlingUnitsBL.setHUStatus(huContext, newCU, X_M_HU.HUSTATUS_Reserved);
			handlingUnitsDAO.saveHU(newCU);
		}

		final HuReservation huReservation = reservationBuilder
				.reservedQtySum(Optional.of(reservedQtySum))
				.build();

		return huReservation;
	}

	/**
	 * Deletes the respective reservation record. VHUs that were split off according to the reservation remain that way for the time being.
	 */
	public void deleteReservation(HuReservationId reservationId)
	{
		throw new NotImplementedException("HuReservationService.deleteReservation");
	}

	/**
	 * Returns a list that is based on the given {@code huIds} and contains the subset of HUs
	 * which are not ruled out because they are reserved for *other* order lines.
	 */
	public List<HuId> retainAvailableHUsForOrderLine(List<HuId> huIds, OrderLineId orderLineId)
	{
		// TODO: avoid the n+1 problem when implementing this
		throw new NotImplementedException("HuReservationService.retainAvailableHUs");
	}

	public ImmutableSet<String> getDocstatusesThatAllowReservation()
	{
		return ImmutableSet.of(
				IDocument.STATUS_Drafted,
				IDocument.STATUS_InProgress,
				IDocument.STATUS_WaitingPayment,
				IDocument.STATUS_Completed);
	}
}
