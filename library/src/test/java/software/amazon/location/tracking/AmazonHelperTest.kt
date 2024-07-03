package software.amazon.location.tracking

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import software.amazon.location.tracking.util.Helper

class AmazonHelperTest {

    private lateinit var context: Context
    private lateinit var helper: Helper
    private lateinit var googleApiAvailability: GoogleApiAvailability

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        helper = Helper()
        googleApiAvailability = mockk(relaxed = true)

        mockkStatic(GoogleApiAvailability::class)
        every { GoogleApiAvailability.getInstance() } returns googleApiAvailability
    }

    @Test
    fun `test Google Play Services are available`() {
        every { googleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SUCCESS

        val result = helper.isGooglePlayServicesAvailable(context)

        assertTrue(result)
    }

    @Test
    fun `test Google Play Services are not available`() {
        every { googleApiAvailability.isGooglePlayServicesAvailable(context) } returns ConnectionResult.SERVICE_MISSING

        val result = helper.isGooglePlayServicesAvailable(context)

        assertFalse(result)
    }
}
